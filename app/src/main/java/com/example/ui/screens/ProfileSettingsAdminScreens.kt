package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.models.AppLanguage
import com.example.models.BackendMode
import com.example.models.BackendSettings
import com.example.models.CropType
import com.example.models.Esp32Device
import com.example.models.UserProfile
import com.example.models.UserRole
import com.example.models.WeatherProviderMode
import com.example.navigation.Routes
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.cropLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    user: UserProfile,
    language: AppLanguage,
    onUpdateProfile: (name: String, district: String, phone: String, crops: List<CropType>) -> Unit,
    onSignOut: () -> Unit
) {
    var name by remember(user) { mutableStateOf(user.displayName) }
    var district by remember(user) { mutableStateOf(user.district) }
    var phone by remember(user) { mutableStateOf(user.phone) }
    var crops by remember(user) {
        mutableStateOf(
            user.cropsGrown.map { CropType.fromRaw(it) }.ifEmpty { listOf(CropType.PADDY) }.toSet()
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Role: ${if (language == AppLanguage.TAMIL) user.resolvedRole().labelTa else user.resolvedRole().labelEn}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.label_full_name)) }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(district, { district = it }, label = { Text(stringResource(R.string.label_district)) }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(phone, { phone = it }, label = { Text(stringResource(R.string.label_phone)) }, modifier = Modifier.fillMaxWidth())
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CropType.entries.forEach { crop ->
                    val selected = crop in crops
                    FilterChip(
                        selected = selected,
                        onClick = {
                            crops = if (selected && crops.size > 1) crops - crop else crops + crop
                        },
                        label = { Text(cropLabel(crop, language)) }
                    )
                }
            }
        }
        item {
            Button(
                onClick = { onUpdateProfile(name, district, phone, crops.toList()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_logout))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: BackendSettings,
    onSaveSettings: (BackendMode, String, WeatherProviderMode, String) -> Unit,
    onOpenLanguage: () -> Unit
) {
    var backendMode by remember(settings) { mutableStateOf(settings.backendMode) }
    var fastApiUrl by remember(settings) { mutableStateOf(settings.fastApiBaseUrl) }
    var weatherMode by remember(settings) { mutableStateOf(settings.weatherProviderMode) }
    var customWeatherUrl by remember(settings) { mutableStateOf(settings.customWeatherUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Backend Adapter Mode", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = backendMode == BackendMode.FIREBASE_CLOUD,
                    onClick = { backendMode = BackendMode.FIREBASE_CLOUD },
                    label = { Text("Firebase Cloud + AI Logic") }
                )
                FilterChip(
                    selected = backendMode == BackendMode.CUSTOM_FASTAPI,
                    onClick = { backendMode = BackendMode.CUSTOM_FASTAPI },
                    label = { Text("Custom FastAPI Backend") }
                )
            }
            if (backendMode == BackendMode.CUSTOM_FASTAPI) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fastApiUrl,
                    onValueChange = { fastApiUrl = it },
                    label = { Text("FastAPI Base URL (HTTPS)") },
                    placeholder = { Text("https://api.your-agroguard-server.example/") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Text("Weather Data Provider", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = weatherMode == WeatherProviderMode.OPEN_METEO_LIVE,
                    onClick = { weatherMode = WeatherProviderMode.OPEN_METEO_LIVE },
                    label = { Text("Open-Meteo Live") }
                )
                FilterChip(
                    selected = weatherMode == WeatherProviderMode.CUSTOM_PROXY,
                    onClick = { weatherMode = WeatherProviderMode.CUSTOM_PROXY },
                    label = { Text("Custom Proxy") }
                )
                FilterChip(
                    selected = weatherMode == WeatherProviderMode.DISABLED,
                    onClick = { weatherMode = WeatherProviderMode.DISABLED },
                    label = { Text("Disabled") }
                )
            }
            if (weatherMode == WeatherProviderMode.CUSTOM_PROXY) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customWeatherUrl,
                    onValueChange = { customWeatherUrl = it },
                    label = { Text("Weather Proxy Base URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Button(
                onClick = { onSaveSettings(backendMode, fastApiUrl, weatherMode, customWeatherUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenLanguage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Language, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.screen_language))
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Deployment & Hardware Setup Guide", style = MaterialTheme.typography.titleMedium)
                    Text("1. Firebase Auth & Firestore: Provisioned via AI Studio Google Sign-In & Enterprise Firestore rules.")
                    Text("2. ESP32 Hardware Ingestion: Deploy server-side HTTPS endpoint (/api/v1/esp32/ingest) with per-device bearer secrets; phone clients cannot forge ESP32_VERIFIED readings.")
                    Text("3. FastAPI Swap: Point FastAPI Base URL above to your deployed server implementing /health, /api/v1/devices/{id}/status, /api/v1/weather, and /api/v1/ai/crop-scan.")
                    Text("4. AI Crop Screening: Powered by Firebase AI Logic (Gemini 3.8 Flash + App Check) with no embedded API keys in the APK.")
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("language_selection_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.screen_language), style = MaterialTheme.typography.headlineSmall)
        AppLanguage.entries.forEach { lang ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectLanguage(lang) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = lang == currentLanguage,
                        onClick = { onSelectLanguage(lang) }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(lang.displayName, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun AdminManagementScreen(
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    devices: List<Esp32Device>,
    onUpdateRole: (UserProfile, UserRole) -> Unit,
    onRevokeDevice: (String) -> Unit
) {
    if (currentUser.resolvedRole() != UserRole.ADMIN) {
        EmptyStateCard(
            icon = Icons.Default.AdminPanelSettings,
            title = "Restricted Access",
            description = "Admin Management is strictly restricted to verified Administrator accounts."
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_management_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("User Role Governance (${allUsers.size})", style = MaterialTheme.typography.titleLarge)
        }
        items(allUsers, key = { it.userId }) { user ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${user.displayName} (${user.role})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${user.email} • ${user.district}", style = MaterialTheme.typography.bodyMedium)
                    if (user.userId != currentUser.userId && user.resolvedRole() != UserRole.ADMIN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onUpdateRole(user, UserRole.FARMER) }) {
                                Text("Set Farmer")
                            }
                            Button(onClick = { onUpdateRole(user, UserRole.EXPERT) }) {
                                Text("Promote to Expert")
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("ESP32 Device Fleet (${devices.size})", style = MaterialTheme.typography.titleLarge)
        }
        items(devices, key = { it.id }) { dev ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${dev.label} (${dev.id})", style = MaterialTheme.typography.titleMedium)
                        Text("Status: ${dev.connectionStatus} • Revoked: ${dev.revoked}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!dev.revoked) {
                        OutlinedButton(onClick = { onRevokeDevice(dev.id) }) {
                            Text("Revoke")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreHubScreen(
    userRole: UserRole,
    onNavigate: (String) -> Unit
) {
    val entries = buildList {
        add(Triple(Routes.IRRIGATION, R.string.screen_irrigation, Icons.Default.WaterDrop))
        add(Triple(Routes.WEATHER_RISK, R.string.screen_weather_risk, Icons.Default.Cloud))
        add(Triple(Routes.ALERTS, R.string.screen_alerts, Icons.Default.NotificationsActive))
        add(Triple(Routes.FARM_ANALYTICS, R.string.screen_analytics, Icons.Default.Analytics))
        add(Triple(Routes.SENSOR_CHARTS, R.string.screen_sensor_charts, Icons.Default.ShowChart))
        add(Triple(Routes.CROP_SELECTION, R.string.screen_crop_selection, Icons.Default.Eco))
        add(Triple(Routes.EXPERT_CONSULTATION, R.string.screen_consultation, Icons.Default.SupportAgent))
        add(Triple(Routes.NOTIFICATIONS, R.string.screen_notifications, Icons.Default.Notifications))
        add(Triple(Routes.PROFILE, R.string.screen_profile, Icons.Default.Person))
        add(Triple(Routes.SETTINGS, R.string.screen_settings, Icons.Default.Settings))
        add(Triple(Routes.LANGUAGE, R.string.screen_language, Icons.Default.Language))
        if (userRole == UserRole.ADMIN) {
            add(Triple(Routes.ADMIN_MANAGEMENT, R.string.screen_admin, Icons.Default.AdminPanelSettings))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_hub_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(entries, key = { it.first }) { (route, titleRes, icon) ->
            HubRow(
                title = stringResource(titleRes),
                icon = icon,
                onClick = { onNavigate(route) },
                tag = "hub_$route"
            )
        }
    }
}

@Composable
private fun HubRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
