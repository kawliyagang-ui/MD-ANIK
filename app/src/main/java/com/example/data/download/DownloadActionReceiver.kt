package com.example.data.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context?.applicationContext ?: return
        val action = intent?.action ?: return
        val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID)?.takeIf { it.isNotBlank() } ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(ctx).mediaDao()
                val engine = DownloadEngine(ctx, dao)
                when (action) {
                    ACTION_PAUSE_DOWNLOAD -> engine.pauseDownload(downloadId)
                    ACTION_CANCEL_DOWNLOAD -> engine.cancelAndRemoveDownload(downloadId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE_DOWNLOAD = "com.example.action.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.action.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }
}
