package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.models.AppLanguage
import com.example.models.BackendMode
import com.example.models.BackendSettings
import com.example.models.WeatherProviderMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.agroGuardDataStore: DataStore<Preferences> by preferencesDataStore(name = "agroguard_prefs")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val BACKEND_MODE = stringPreferencesKey("backend_mode")
        val FASTAPI_URL = stringPreferencesKey("fastapi_base_url")
        val WEATHER_MODE = stringPreferencesKey("weather_provider_mode")
        val CUSTOM_WEATHER_URL = stringPreferencesKey("custom_weather_url")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_SYNCED_MS = longPreferencesKey("last_synced_epoch_ms")
        val SELECTED_FARM_ID = stringPreferencesKey("selected_farm_id")
    }

    val settingsFlow: Flow<BackendSettings> = context.agroGuardDataStore.data.map { prefs ->
        BackendSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            backendMode = prefs[Keys.BACKEND_MODE]?.let { raw ->
                BackendMode.entries.firstOrNull { it.name == raw }
            } ?: BackendMode.FIREBASE_CLOUD,
            fastApiBaseUrl = prefs[Keys.FASTAPI_URL].orEmpty(),
            weatherProviderMode = prefs[Keys.WEATHER_MODE]?.let { raw ->
                WeatherProviderMode.entries.firstOrNull { it.name == raw }
            } ?: WeatherProviderMode.OPEN_METEO_LIVE,
            customWeatherUrl = prefs[Keys.CUSTOM_WEATHER_URL].orEmpty(),
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            lastSyncedEpochMs = prefs[Keys.LAST_SYNCED_MS] ?: 0L,
            selectedFarmId = prefs[Keys.SELECTED_FARM_ID].orEmpty()
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.agroGuardDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.agroGuardDataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun setSelectedFarmId(farmId: String) {
        context.agroGuardDataStore.edit { it[Keys.SELECTED_FARM_ID] = farmId }
    }

    suspend fun updateBackendConfig(
        backendMode: BackendMode,
        fastApiBaseUrl: String,
        weatherProviderMode: WeatherProviderMode,
        customWeatherUrl: String
    ) {
        context.agroGuardDataStore.edit {
            it[Keys.BACKEND_MODE] = backendMode.name
            it[Keys.FASTAPI_URL] = fastApiBaseUrl.trim()
            it[Keys.WEATHER_MODE] = weatherProviderMode.name
            it[Keys.CUSTOM_WEATHER_URL] = customWeatherUrl.trim()
        }
    }

    suspend fun updateLastSynced(epochMs: Long) {
        context.agroGuardDataStore.edit { it[Keys.LAST_SYNCED_MS] = epochMs }
    }
}
