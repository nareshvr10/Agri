package com.example.database

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.SettingsDataStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class AgroGuardSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = AgroGuardDatabase.getInstance(applicationContext)
        val pendingDao = db.pendingSyncDao()
        val settings = SettingsDataStore(applicationContext)
        val auth = Firebase.auth
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.success()
        }

        val firestore = FirebaseFirestore.getInstance(
            applicationContext.getString(R.string.firestore_database_id)
        )

        val pending = pendingDao.getPendingNow()
        var hadFailure = false

        for (item in pending) {
            try {
                val json = JSONObject(item.payloadJson)
                when (item.entityType) {
                    "ALERT_ACK" -> {
                        firestore.collection("alerts").document(item.documentId)
                            .update(
                                mapOf(
                                    "acknowledged" to json.optBoolean("acknowledged", true),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        pendingDao.deleteById(item.queueId)
                    }
                    "ZONE_THRESHOLDS" -> {
                        firestore.collection("crop_zones").document(item.documentId)
                            .update(
                                mapOf(
                                    "minSoilMoisturePct" to json.getDouble("minSoilMoisturePct"),
                                    "maxSoilMoisturePct" to json.getDouble("maxSoilMoisturePct"),
                                    "maxAirTempC" to json.getDouble("maxAirTempC"),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        pendingDao.deleteById(item.queueId)
                    }
                    else -> pendingDao.deleteById(item.queueId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Deferred sync failed for ${item.entityType}/${item.documentId}", e)
                hadFailure = true
            }
        }

        settings.updateLastSynced(System.currentTimeMillis())
        return if (hadFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "AgroGuardSyncWorker"
        private const val UNIQUE_WORK_NAME = "agroguard_offline_sync"

        fun enqueueWhenConnected(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<AgroGuardSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
