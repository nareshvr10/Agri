package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.NetworkMonitor
import com.example.data.SettingsDataStore
import com.example.models.AppLanguage
import com.example.models.BackendMode
import com.example.models.BackendSettings
import com.example.models.WeatherProviderMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val settings: StateFlow<BackendSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = BackendSettings()
        )

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = networkMonitor.isOnlineNow()
        )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
        }
    }

    fun toggleLanguage() {
        val next = if (settings.value.language == AppLanguage.ENGLISH) {
            AppLanguage.TAMIL
        } else {
            AppLanguage.ENGLISH
        }
        setLanguage(next)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
        }
    }

    fun selectFarm(farmId: String) {
        viewModelScope.launch {
            settingsDataStore.setSelectedFarmId(farmId)
        }
    }

    fun updateBackendConfig(
        backendMode: BackendMode,
        fastApiBaseUrl: String,
        weatherProviderMode: WeatherProviderMode,
        customWeatherUrl: String
    ) {
        viewModelScope.launch {
            settingsDataStore.updateBackendConfig(
                backendMode = backendMode,
                fastApiBaseUrl = fastApiBaseUrl,
                weatherProviderMode = weatherProviderMode,
                customWeatherUrl = customWeatherUrl
            )
        }
    }
}
