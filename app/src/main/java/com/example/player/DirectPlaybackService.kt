package com.example.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Process-local bridge holding the active direct ExoPlayer so [DirectMediaPlayerScreen]
 * renders video directly on [androidx.media3.ui.PlayerView] with zero Binder startup latency
 * while [DirectPlaybackService] binds a Media3 [MediaSession] for background audio play and
 * rich lock-screen/notification controls.
 */
object DirectPlaybackHolder {

    @Volatile
    private var activePlayer: ExoPlayer? = null

    @Volatile
    private var activeOwnerToken: String? = null

    @Volatile
    private var activeSourceKey: String? = null

    @Volatile
    private var onStopCallback: (() -> Unit)? = null

    fun setPlayer(
        ownerToken: String,
        sourceKey: String,
        player: ExoPlayer,
        onStopRequested: (() -> Unit)? = null
    ) {
        val previous = activePlayer
        if (previous != null && previous !== player) {
            runCatching { previous.release() }
        }
        activeOwnerToken = ownerToken
        activeSourceKey = sourceKey
        activePlayer = player
        onStopCallback = onStopRequested
    }

    fun getPlayer(): ExoPlayer? = activePlayer

    fun getOrCreateFallbackPlayer(context: Context): ExoPlayer {
        activePlayer?.let { return it }
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        return ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { activePlayer = it }
    }

    fun requestStopFromNotification() {
        val player = activePlayer
        runCatching {
            player?.playWhenReady = false
            player?.stop()
        }
        onStopCallback?.invoke()
    }

    fun releaseIfOwner(ownerToken: String) {
        if (activeOwnerToken != ownerToken) return
        val player = activePlayer
        activePlayer = null
        activeOwnerToken = null
        activeSourceKey = null
        onStopCallback = null
        runCatching { player?.release() }
    }
}

@OptIn(UnstableApi::class)
class DirectPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.direct_player_notification_channel)
            .build()
            .apply {
                setSmallIcon(R.mipmap.ic_launcher)
            }
        setMediaNotificationProvider(notificationProvider)

        val player = DirectPlaybackHolder.getOrCreateFallbackPlayer(this)

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            SESSION_REQUEST_CODE,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeCommand = SessionCommand(ACTION_CLOSE_PLAYER, Bundle.EMPTY)
        val closeButton = CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName(getString(R.string.direct_player_notification_close))
            .setSessionCommand(closeCommand)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .setSessionActivity(sessionActivityPendingIntent)
            .setMediaButtonPreferences(listOf(closeButton))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(closeCommand)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == ACTION_CLOSE_PLAYER) {
                        DirectPlaybackHolder.requestStopFromNotification()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val currentSession = mediaSession ?: return null
        val holderPlayer = DirectPlaybackHolder.getPlayer()
        if (holderPlayer != null && currentSession.player !== holderPlayer) {
            currentSession.player = holderPlayer
        }
        return currentSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "cinetra_direct_playback"
        private const val SESSION_ID = "cinetra_direct_playback_session"
        private const val SESSION_REQUEST_CODE = 4409
        const val ACTION_CLOSE_PLAYER = "com.example.player.ACTION_CLOSE_DIRECT_PLAYER"

        fun buildMediaMetadata(
            title: String,
            subtitle: String,
            artworkUri: android.net.Uri?
        ): MediaMetadata {
            return MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(subtitle)
                .apply {
                    if (artworkUri != null) {
                        setArtworkUri(artworkUri)
                    }
                }
                .build()
        }

        fun updateSessionMediaMetadata(
            player: ExoPlayer,
            metadata: MediaMetadata
        ) {
            val currentItem = player.currentMediaItem ?: return
            val updated: Media3Item = currentItem.buildUpon()
                .setMediaMetadata(metadata)
                .build()
            player.replaceMediaItem(player.currentMediaItemIndex, updated)
        }
    }
}
