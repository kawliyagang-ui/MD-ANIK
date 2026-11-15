package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    private const val FALLBACK_DATABASE_ID = "ai-studio-android-streamfl-b7ad1e10-a40a-4c27-a9c4-067cca6a6453"
    private const val FALLBACK_PROJECT_ID = "gen-lang-client-0724965998"
    private const val FALLBACK_APP_ID = "1:238431570403:android:b3be54388e1c8cfd2678d6"
    private const val FALLBACK_API_KEY = "AIzaSyC_BQFY94rC4pRI_WAPK1KPHoPmVJiBDZ0"
    private const val FALLBACK_GCM_SENDER_ID = "238431570403"
    private const val FALLBACK_STORAGE_BUCKET = "gen-lang-client-0724965998.firebasestorage.app"

    fun resolveDatabaseId(context: Context?): String {
        if (context == null) return FALLBACK_DATABASE_ID
        return runCatching {
            context.applicationContext.getString(R.string.firestore_database_id)
                .takeIf { it.isNotBlank() }
        }.getOrNull() ?: FALLBACK_DATABASE_ID
    }

    fun ensureAppInitialized(context: Context?): FirebaseApp? {
        if (context == null) {
            return runCatching { FirebaseApp.getInstance() }.getOrNull()
        }
        val appContext = context.applicationContext
        val existing = runCatching { FirebaseApp.getApps(appContext).firstOrNull() }.getOrNull()
        if (existing != null) return existing

        val fromResources = runCatching { FirebaseApp.initializeApp(appContext) }.getOrNull()
        if (fromResources != null) return fromResources

        return runCatching {
            val options = FirebaseOptions.Builder()
                .setProjectId(FALLBACK_PROJECT_ID)
                .setApplicationId(FALLBACK_APP_ID)
                .setApiKey(FALLBACK_API_KEY)
                .setGcmSenderId(FALLBACK_GCM_SENDER_ID)
                .setStorageBucket(FALLBACK_STORAGE_BUCKET)
                .build()
            FirebaseApp.initializeApp(appContext, options)
        }.onFailure { err ->
            Log.e(TAG, "Failed to initialize FirebaseApp", err)
        }.getOrNull()
    }

    fun getFirestore(context: Context?): FirebaseFirestore? {
        val app = ensureAppInitialized(context) ?: return null
        val databaseId = resolveDatabaseId(context)
        return runCatching {
            FirebaseFirestore.getInstance(app, databaseId).apply {
                runCatching {
                    firestoreSettings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build()
                }
            }
        }.onFailure { err ->
            Log.e(TAG, "Failed to obtain FirebaseFirestore instance for db=$databaseId", err)
        }.getOrNull()
    }
}
