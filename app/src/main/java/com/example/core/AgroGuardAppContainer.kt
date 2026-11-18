package com.example.core

import android.content.Context
import com.example.R
import com.example.data.AiDiagnosisRepository
import com.example.data.AlertRepository
import com.example.data.ConsultationRepository
import com.example.data.FarmRepository
import com.example.data.FirebaseAiDiagnosisRepository
import com.example.data.FirebaseAlertRepository
import com.example.data.FirebaseConsultationRepository
import com.example.data.FirebaseFarmRepository
import com.example.data.FirebaseScanRepository
import com.example.data.FirebaseSensorRepository
import com.example.data.FirebaseUserAndAdminRepository
import com.example.data.LiveWeatherRepository
import com.example.data.ScanRepository
import com.example.data.SensorRepository
import com.example.data.SettingsDataStore
import com.example.data.UserAndAdminRepository
import com.example.data.WeatherRepository
import com.example.database.AgroGuardDatabase
import com.google.firebase.firestore.FirebaseFirestore

class AgroGuardAppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    val localDatabase: AgroGuardDatabase = AgroGuardDatabase.getInstance(appContext)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(appContext)
    val networkMonitor: NetworkMonitor = NetworkMonitor(appContext)

    val farmRepository: FarmRepository =
        FirebaseFarmRepository(firestore, localDatabase, settingsDataStore, appContext)
    val sensorRepository: SensorRepository =
        FirebaseSensorRepository(firestore, localDatabase)
    val alertRepository: AlertRepository =
        FirebaseAlertRepository(firestore, localDatabase, appContext)
    val scanRepository: ScanRepository =
        FirebaseScanRepository(firestore, localDatabase)
    val userAndAdminRepository: UserAndAdminRepository =
        FirebaseUserAndAdminRepository(firestore)
    val consultationRepository: ConsultationRepository =
        FirebaseConsultationRepository(firestore)
    val aiDiagnosisRepository: AiDiagnosisRepository =
        FirebaseAiDiagnosisRepository()
    val weatherRepository: WeatherRepository =
        LiveWeatherRepository()
}
