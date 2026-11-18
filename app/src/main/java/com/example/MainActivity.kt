package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.AgroGuardAppContainer
import com.example.core.ProvideLocalizedContext
import com.example.core.configureAppCheck
import com.example.navigation.AgroGuardNavHost
import com.example.ui.theme.AgroGuardTheme
import com.example.viewmodel.AppSettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure App Check immediately before setContent / ViewModel initialization
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val container = remember { AgroGuardAppContainer(applicationContext) }
            val settingsViewModel: AppSettingsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        AppSettingsViewModel(
                            settingsDataStore = container.settingsDataStore,
                            networkMonitor = container.networkMonitor
                        )
                    }
                }
            )
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            ProvideLocalizedContext(language = settings.language) {
                AgroGuardTheme {
                    AgroGuardNavHost(
                        container = container,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
