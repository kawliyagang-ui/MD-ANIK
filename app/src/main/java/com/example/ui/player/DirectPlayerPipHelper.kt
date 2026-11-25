package com.example.ui.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val ACTION_PIP_CONTROL = "com.example.player.ACTION_PIP_CONTROL"
private const val EXTRA_PIP_CONTROL_TYPE = "control_type"
private const val PIP_CONTROL_PLAY = 1
private const val PIP_CONTROL_PAUSE = 2

fun isPictureInPictureSupported(context: Context): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

fun buildClampedAspectRational(videoWidth: Int, videoHeight: Int): Rational {
    if (videoWidth <= 0 || videoHeight <= 0) {
        return Rational(16, 9)
    }
    val ratio = videoWidth.toFloat() / videoHeight.toFloat()
    return when {
        ratio > 2.35f -> Rational(235, 100)
        ratio < 0.43f -> Rational(43, 100)
        else -> Rational(videoWidth, videoHeight)
    }
}

private fun buildPipRemoteActions(context: Context, isPlaying: Boolean): List<RemoteAction> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
    val controlType = if (isPlaying) PIP_CONTROL_PAUSE else PIP_CONTROL_PLAY
    val iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
    val title = if (isPlaying) "Pause" else "Play"
    val intent = Intent(ACTION_PIP_CONTROL).apply {
        setPackage(context.packageName)
        putExtra(EXTRA_PIP_CONTROL_TYPE, controlType)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        controlType,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return listOf(
        RemoteAction(
            Icon.createWithResource(context, iconRes),
            title,
            title,
            pendingIntent
        )
    )
}

fun updatePictureInPictureParams(
    activity: Activity?,
    isPlaying: Boolean,
    shouldAutoEnter: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    sourceRect: Rect?
) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (!isPictureInPictureSupported(activity)) return
    runCatching {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(buildClampedAspectRational(videoWidth, videoHeight))
            .setActions(buildPipRemoteActions(activity, isPlaying))
        if (sourceRect != null && !sourceRect.isEmpty) {
            builder.setSourceRectHint(sourceRect)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(shouldAutoEnter)
        }
        activity.setPictureInPictureParams(builder.build())
    }
}

fun enterPictureInPictureNow(
    activity: Activity?,
    isPlaying: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    sourceRect: Rect?
): Boolean {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    if (!isPictureInPictureSupported(activity)) return false
    return runCatching {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(buildClampedAspectRational(videoWidth, videoHeight))
            .setActions(buildPipRemoteActions(activity, isPlaying))
        if (sourceRect != null && !sourceRect.isEmpty) {
            builder.setSourceRectHint(sourceRect)
        }
        activity.enterPictureInPictureMode(builder.build())
    }.getOrDefault(false)
}

@Composable
fun DirectPlayerPipEffects(
    activity: Activity?,
    isPlaying: Boolean,
    shouldAutoEnterPip: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    sourceRect: Rect?,
    onPipModeChanged: (Boolean) -> Unit,
    onPipClosedByUser: () -> Unit,
    onPipPlay: () -> Unit,
    onPipPause: () -> Unit
) {
    val componentActivity = activity as? ComponentActivity ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentShouldAutoEnter by rememberUpdatedState(shouldAutoEnterPip)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentWidth by rememberUpdatedState(videoWidth)
    val currentHeight by rememberUpdatedState(videoHeight)
    val currentSourceRect by rememberUpdatedState(sourceRect)
    val currentOnPipModeChanged by rememberUpdatedState(onPipModeChanged)
    val currentOnPipClosedByUser by rememberUpdatedState(onPipClosedByUser)
    val currentOnPipPlay by rememberUpdatedState(onPipPlay)
    val currentOnPipPause by rememberUpdatedState(onPipPause)

    // Keep Activity PiP params synchronized (including setAutoEnterEnabled on Android 12+)
    DisposableEffect(componentActivity, isPlaying, shouldAutoEnterPip, videoWidth, videoHeight, sourceRect) {
        updatePictureInPictureParams(
            activity = componentActivity,
            isPlaying = isPlaying,
            shouldAutoEnter = shouldAutoEnterPip,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            sourceRect = sourceRect
        )
        onDispose { }
    }

    // Ensure auto-enter is disabled when exiting the direct player screen
    DisposableEffect(componentActivity) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    val reset = PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(false)
                        .build()
                    componentActivity.setPictureInPictureParams(reset)
                }
            }
        }
    }

    // Listen to PiP transitions & detect user closing the floating PiP window
    DisposableEffect(componentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentOnPipModeChanged(componentActivity.isInPictureInPictureMode)
        }
        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            currentOnPipModeChanged(info.isInPictureInPictureMode)
            if (!info.isInPictureInPictureMode &&
                lifecycleOwner.lifecycle.currentState == Lifecycle.State.CREATED
            ) {
                currentOnPipClosedByUser()
            }
        }
        componentActivity.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            componentActivity.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    // Pre-Android 12 user-leave-hint fallback to enter PiP on Home press while playing
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S) {
        DisposableEffect(componentActivity) {
            val leaveHintListener = Runnable {
                if (currentShouldAutoEnter) {
                    enterPictureInPictureNow(
                        activity = componentActivity,
                        isPlaying = currentIsPlaying,
                        videoWidth = currentWidth,
                        videoHeight = currentHeight,
                        sourceRect = currentSourceRect
                    )
                }
            }
            componentActivity.addOnUserLeaveHintListener(leaveHintListener)
            onDispose {
                componentActivity.removeOnUserLeaveHintListener(leaveHintListener)
            }
        }
    }

    // Broadcast receiver for Play / Pause RemoteActions in the PiP window
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        DisposableEffect(componentActivity) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action != ACTION_PIP_CONTROL) return
                    when (intent.getIntExtra(EXTRA_PIP_CONTROL_TYPE, 0)) {
                        PIP_CONTROL_PLAY -> currentOnPipPlay()
                        PIP_CONTROL_PAUSE -> currentOnPipPause()
                    }
                }
            }
            ContextCompat.registerReceiver(
                componentActivity,
                receiver,
                IntentFilter(ACTION_PIP_CONTROL),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            onDispose {
                runCatching { componentActivity.unregisterReceiver(receiver) }
            }
        }
    }
}
