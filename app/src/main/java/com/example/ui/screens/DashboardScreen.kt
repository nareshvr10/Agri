package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.models.AppLanguage
import com.example.models.CropScanRecord
import com.example.models.CropZone
import com.example.models.DeviceConnectionStatus
import com.example.models.Esp32Device
import com.example.models.Farm
import com.example.models.FarmAlert
import com.example.models.IrrigationRecommendation
import com.example.models.SensorReading
import com.example.models.TelemetrySource
import com.example.models.UserProfile
import com.example.models.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfflineSyncBanner
import com.example.ui.components.TelemetrySourceChip
import com.example.ui.components.cropLabel
import com.example.ui.components.formatEpochShort
import com.example.ui.components.stageLabel
import com.example.ui.theme.ForestDeep

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    user: UserProfile,
    farms: List<Farm>,
    selectedFarm: Farm?,
    zones: List<CropZone>,
    devices: List<Esp32Device>,
    readings: List<SensorReading>,
    alerts: List<FarmAlert>,
    scans: List<CropScanRecord>,
    irrigationPreview: IrrigationRecommendation?,
    language: AppLanguage,
    isOnline: Boolean,
    lastSyncedEpochMs: Long,
    onSelectFarm: (String) -> Unit,
    onOpenAddFarm: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenSensors: () -> Unit,
    onOpenIrrigation: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenConsultation: () -> Unit,
    onOpenCropZones: () -> Unit
) {
    val farmZones = selectedFarm?.let { f -> zones.filter { it.farmId == f.id } }.orEmpty()
    val farmDevices = selectedFarm?.let { f -> devices.filter { it.farmId == f.id } }.orEmpty()
    val farmReadings = selectedFarm?.let { f -> readings.filter { it.farmId == f.id } }.orEmpty()
    val latestReading = farmReadings.maxByOrNull { it.recordedEpochMs }
    val activeDevice = farmDevices.firstOrNull {
        it.resolvedStatus() == DeviceConnectionStatus.ONLINE && !it.revoked
    }
    val farmAlerts = selectedFarm?.let { f -> alerts.filter { it.farmId == f.id && !it.acknowledged } }.orEmpty()
    val farmScans = selectedFarm?.let { f -> scans.filter { it.farmId == f.id } }.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OfflineSyncBanner(isOnline = isOnline, lastSyncedEpochMs = lastSyncedEpochMs)
        }

        // Sunrise Hero Banner with Uploaded Image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.hero_agroguard_sunrise),
                    contentDescription = "AgroGuard AI Farm Sunrise Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ForestDeep.copy(alpha = 0.88f),
                                    ForestDeep.copy(alpha = 0.25f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (language == AppLanguage.TAMIL) user.resolvedRole().labelTa else user.resolvedRole().labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Vanakkam, ${user.displayName}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = selectedFarm?.let { "${it.name} • ${it.locationName}" }
                            ?: stringResource(R.string.dashboard_no_farms_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD8F3DC)
                    )
                }
            }
        }

        // Selected Farm Switcher
        if (farms.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.dashboard_selected_farm),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(farms, key = { it.id }) { farm ->
                            FilterChip(
                                selected = farm.id == selectedFarm?.id,
                                onClick = { onSelectFarm(farm.id) },
                                label = {
                                    Text("${farm.name} (${cropLabel(farm.resolvedCrop(), language)})")
                                }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Eco,
                    title = stringResource(R.string.dashboard_no_farms_title),
                    description = stringResource(R.string.dashboard_no_farms_desc),
                    action = {
                        if (user.resolvedRole() != UserRole.EXPERT) {
                            Button(
                                onClick = onOpenAddFarm,
                                modifier = Modifier.testTag("dashboard_add_farm_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.screen_add_farm))
                            }
                        }
                    }
                )
            }
        }

        // Quick Actions Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.dashboard_quick_actions),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionPill(Icons.Default.CameraAlt, stringResource(R.string.nav_scanner), onOpenScanner, "qa_scan")
                    QuickActionPill(Icons.Default.WaterDrop, stringResource(R.string.screen_irrigation), onOpenIrrigation, "qa_irrigation")
                    QuickActionPill(Icons.Default.Cloud, stringResource(R.string.screen_weather_risk), onOpenWeather, "qa_weather")
                    QuickActionPill(Icons.Default.Sensors, stringResource(R.string.nav_sensors), onOpenSensors, "qa_sensors")
                    QuickActionPill(Icons.Default.SupportAgent, stringResource(R.string.screen_consultation), onOpenConsultation, "qa_consult")
                    if (user.resolvedRole() != UserRole.EXPERT) {
                        QuickActionPill(Icons.Default.Add, stringResource(R.string.screen_add_farm), onOpenAddFarm, "qa_add_farm")
                    }
                }
            }
        }

        // Latest Actual Sensor Readings OR Honest "Device not connected"
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_sensor_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_latest_sensors),
                            style = MaterialTheme.typography.titleMedium
                        )
                        val badgeSource = when {
                            latestReading == null && activeDevice == null -> TelemetrySource.UNCONNECTED
                            latestReading != null -> latestReading.resolvedSource()
                            else -> TelemetrySource.ESP32_VERIFIED
                        }
                        TelemetrySourceChip(
                            source = badgeSource,
                            language = language,
                            isCachedOffline = !isOnline && latestReading != null
                        )
                    }

                    if (latestReading == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SensorsOff,
                                contentDescription = stringResource(R.string.status_device_not_connected),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.status_device_not_connected),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("device_not_connected_label")
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_no_sensor_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(onClick = onOpenSensors) {
                            Text(stringResource(R.string.screen_sensor_monitoring))
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile("Soil Moisture", "${"%.1f".format(latestReading.soilMoisturePct)} %")
                            MetricTile("Air Temp", "${"%.1f".format(latestReading.airTempC)} °C")
                            MetricTile("Humidity", "${"%.0f".format(latestReading.humidityPct)} %")
                            MetricTile("Soil Temp", "${"%.1f".format(latestReading.soilTempC)} °C")
                            MetricTile("Rainfall", "${"%.1f".format(latestReading.rainfallMm)} mm")
                        }
                        Text(
                            text = "Recorded: ${formatEpochShort(latestReading.recordedEpochMs)} • Device: ${latestReading.deviceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Crop Status & Zones Summary
        if (selectedFarm != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCropZones() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.screen_crop_zones),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${farmZones.size} zones",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (farmZones.isEmpty()) {
                            Text(
                                text = "No crop zones configured for ${selectedFarm.name}. Tap to add Paddy, Banana, or Tomato zone details.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            farmZones.take(3).forEach { zone ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${zone.name} (${cropLabel(zone.resolvedCrop(), language)})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stageLabel(zone.resolvedStage(), language),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Irrigation Status Preview
        if (irrigationPreview != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenIrrigation() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_irrigation_status),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (language == AppLanguage.TAMIL) irrigationPreview.actionSummaryTa else irrigationPreview.actionSummaryEn,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${irrigationPreview.dataFreshnessNote} • ${irrigationPreview.confidenceNote}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.irrigation_no_actuation_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Alerts Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlerts() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dashboard_active_alerts),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${farmAlerts.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (farmAlerts.isEmpty()) {
                        Text(
                            text = "No unacknowledged alerts for this farm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        farmAlerts.take(2).forEach { alert ->
                            Text(
                                text = "• [${alert.severity}] ${alert.title}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Recent AI Assessments
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_recent_scans),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (farmScans.isEmpty()) {
                        Text(
                            text = "No AI crop leaf scans recorded yet. Use AI Crop Scanner to inspect Paddy, Banana, or Tomato leaves.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        farmScans.take(3).forEach { scan ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scan.possibleCondition,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${cropLabel(scan.resolvedCrop(), language)} • ${scan.confidencePct}% • ${if (scan.expertConfirmed) "Expert Verified" else "Preliminary"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun MetricTile(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
