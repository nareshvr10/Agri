package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CropGuideCatalog
import com.example.models.AppLanguage
import com.example.models.CropZone
import com.example.models.Esp32Device
import com.example.models.Farm
import com.example.models.SensorReading
import com.example.models.TelemetrySource
import com.example.models.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TelemetryChartCard
import com.example.ui.components.TelemetrySourceChip
import com.example.ui.components.formatEpochShort
import com.example.ui.theme.EarthLoam
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.LeafGreen

@Composable
fun SensorMonitoringScreen(
    selectedFarm: Farm?,
    zones: List<CropZone>,
    devices: List<Esp32Device>,
    readings: List<SensorReading>,
    userRole: UserRole,
    language: AppLanguage,
    onRegisterDevice: (farm: Farm, zone: CropZone, deviceId: String, label: String, firmware: String) -> Unit,
    onRevokeDevice: (String) -> Unit,
    onRecordManualReading: (
        farm: Farm,
        zone: CropZone,
        soilMoist: Double,
        soilTemp: Double,
        airTemp: Double,
        humidity: Double,
        lux: Double,
        rain: Double,
        waterLevel: Double?
    ) -> Unit,
    onOpenCharts: () -> Unit
) {
    val farmZones = selectedFarm?.let { f -> zones.filter { it.farmId == f.id } }.orEmpty()
    val farmDevices = selectedFarm?.let { f -> devices.filter { it.farmId == f.id } }.orEmpty()
    val farmReadings = selectedFarm?.let { f -> readings.filter { it.farmId == f.id } }.orEmpty()

    var showDeviceForm by remember { mutableStateOf(false) }
    var showManualForm by remember { mutableStateOf(false) }
    var deviceIdText by remember { mutableStateOf("esp32-node-01") }
    var deviceLabelText by remember { mutableStateOf("North Paddy Node") }
    var firmwareText by remember { mutableStateOf("v1.0.0") }

    var moistText by remember { mutableStateOf("58.0") }
    var soilTempText by remember { mutableStateOf("26.5") }
    var airTempText by remember { mutableStateOf("30.0") }
    var humText by remember { mutableStateOf("70.0") }
    var luxText by remember { mutableStateOf("42000") }
    var rainText by remember { mutableStateOf("0.0") }
    var waterText by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sensor_monitoring_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(R.string.esp32_security_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (selectedFarm == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.SensorsOff,
                    title = stringResource(R.string.status_device_not_connected),
                    description = "Select or add a farm first to manage ESP32 nodes."
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (userRole != UserRole.EXPERT && farmZones.isNotEmpty()) {
                        Button(
                            onClick = { showDeviceForm = !showDeviceForm },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (showDeviceForm) "Close" else "Link ESP32")
                        }
                        OutlinedButton(
                            onClick = { showManualForm = !showManualForm },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (showManualForm) "Close" else "Manual Log")
                        }
                    }
                    OutlinedButton(onClick = onOpenCharts) {
                        Icon(Icons.Default.ShowChart, contentDescription = null)
                    }
                }
            }

            if (showDeviceForm && farmZones.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Register ESP32 Node", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = deviceIdText,
                                onValueChange = { deviceIdText = it },
                                label = { Text("Device ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = deviceLabelText,
                                onValueChange = { deviceLabelText = it },
                                label = { Text("Device Label") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = firmwareText,
                                onValueChange = { firmwareText = it },
                                label = { Text("Firmware Version") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onRegisterDevice(selectedFarm, farmZones.first(), deviceIdText, deviceLabelText, firmwareText)
                                    showDeviceForm = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Register Device")
                            }
                        }
                    }
                }
            }

            if (showManualForm && farmZones.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Record Manual Field Observation", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(moistText, { moistText = it }, label = { Text("Soil Moist %") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(soilTempText, { soilTempText = it }, label = { Text("Soil Temp °C") }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(airTempText, { airTempText = it }, label = { Text("Air Temp °C") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(humText, { humText = it }, label = { Text("Humidity %") }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(luxText, { luxText = it }, label = { Text("Light lux") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(rainText, { rainText = it }, label = { Text("Rainfall mm") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(waterText, { waterText = it }, label = { Text("Water Level cm (Optional)") }, modifier = Modifier.fillMaxWidth())
                            if (formError != null) {
                                Text(formError.orEmpty(), color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = {
                                    val m = moistText.toDoubleOrNull()
                                    val st = soilTempText.toDoubleOrNull()
                                    val at = airTempText.toDoubleOrNull()
                                    val h = humText.toDoubleOrNull()
                                    val l = luxText.toDoubleOrNull()
                                    val r = rainText.toDoubleOrNull()
                                    val w = waterText.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
                                    if (m == null || st == null || at == null || h == null || l == null || r == null) {
                                        formError = "All primary sensor numbers must be valid decimals."
                                    } else {
                                        formError = null
                                        onRecordManualReading(selectedFarm, farmZones.first(), m, st, at, h, l, r, w)
                                        showManualForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Manual Field Log")
                            }
                        }
                    }
                }
            }

            if (farmDevices.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.SensorsOff,
                        title = stringResource(R.string.status_device_not_connected),
                        description = "No ESP32 hardware nodes registered for ${selectedFarm.name}."
                    )
                }
            } else {
                items(farmDevices, key = { it.id }) { dev ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${dev.label} (${dev.id})", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = dev.connectionStatus,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (dev.revoked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Zone: ${dev.cropZoneId} • FW: ${dev.firmwareVersion} • Key Hint: ${dev.credentialHint}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Last seen: ${formatEpochShort(dev.lastSeenEpochMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!dev.revoked && userRole != UserRole.EXPERT) {
                                OutlinedButton(onClick = { onRevokeDevice(dev.id) }) {
                                    Icon(Icons.Default.Block, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Revoke Device Token")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Configurable Sensor Units", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CropGuideCatalog.sensorMetadata.forEach { spec ->
                        Text(
                            text = "• ${if (language == AppLanguage.TAMIL) spec.labelTa else spec.labelEn}: ${spec.unit} (${spec.minValid}..${spec.maxValid})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (farmReadings.isNotEmpty()) {
                item {
                    Text("Recent Readings (${farmReadings.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(farmReadings.take(10), key = { it.id }) { reading ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatEpochShort(reading.recordedEpochMs),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TelemetrySourceChip(source = reading.resolvedSource(), language = language)
                            }
                            Text(
                                text = "Moisture ${reading.soilMoisturePct}% • Soil ${reading.soilTempC}°C • Air ${reading.airTempC}°C • RH ${reading.humidityPct}% • Rain ${reading.rainfallMm}mm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorHistoryChartsScreen(
    selectedFarm: Farm?,
    readings: List<SensorReading>
) {
    val farmReadings = selectedFarm?.let { f ->
        readings.filter { it.farmId == f.id }.sortedBy { it.recordedEpochMs }
    }.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sensor_charts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TelemetryChartCard(
                title = "Soil Moisture Trend",
                unit = "%",
                points = farmReadings.map { it.recordedEpochMs to it.soilMoisturePct },
                lineColor = LeafGreen
            )
        }
        item {
            TelemetryChartCard(
                title = "Air Temperature Trend",
                unit = "°C",
                points = farmReadings.map { it.recordedEpochMs to it.airTempC },
                lineColor = HarvestAmber
            )
        }
        item {
            TelemetryChartCard(
                title = "Relative Humidity Trend",
                unit = "%",
                points = farmReadings.map { it.recordedEpochMs to it.humidityPct },
                lineColor = EarthLoam
            )
        }
        item {
            TelemetryChartCard(
                title = "Rainfall History",
                unit = "mm",
                points = farmReadings.map { it.recordedEpochMs to it.rainfallMm },
                lineColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}
