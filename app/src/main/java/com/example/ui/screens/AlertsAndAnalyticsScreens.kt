package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.AlertSeverity
import com.example.models.CropScanRecord
import com.example.models.Farm
import com.example.models.FarmAlert
import com.example.models.SensorReading
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TelemetryChartCard
import com.example.ui.components.formatEpochShort
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.EarthLoam
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestAmberContainer
import com.example.ui.theme.LeafGreen

@Composable
fun AlertsScreen(
    alerts: List<FarmAlert>,
    isOnline: Boolean,
    onAcknowledge: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("alerts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (alerts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.NotificationsNone,
                    title = "No Alerts Recorded",
                    description = "Soil moisture, high temperature, heavy rainfall, sensor offline, and crop health alerts appear here."
                )
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                val (bg, fg) = when (alert.resolvedSeverity()) {
                    AlertSeverity.CRITICAL -> AlertCrimsonContainer to AlertCrimson
                    AlertSeverity.WARNING -> HarvestAmberContainer to Color(0xFF78350F)
                    AlertSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = bg, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "${alert.severity} • ${alert.alertType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = formatEpochShort(alert.createdAt?.toDate()?.time ?: 0L),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(alert.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(alert.explanation, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Farm: ${alert.farmId} • Zone: ${alert.cropZoneId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!alert.acknowledged) {
                            Button(onClick = { onAcknowledge(alert.id, isOnline) }) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Text("Acknowledge")
                            }
                        } else {
                            Text(
                                text = "Acknowledged",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class DateRangeFilter(val label: String, val windowMs: Long?) {
    DAY("24 Hours", 24 * 60 * 60 * 1000L),
    WEEK("7 Days", 7 * 24 * 60 * 60 * 1000L),
    MONTH("30 Days", 30 * 24 * 60 * 60 * 1000L),
    ALL("All", null)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmAnalyticsScreen(
    selectedFarm: Farm?,
    readings: List<SensorReading>,
    alerts: List<FarmAlert>,
    scans: List<CropScanRecord>
) {
    var range by remember { mutableStateOf(DateRangeFilter.ALL) }
    val nowMs = remember { System.currentTimeMillis() }

    val farmReadings = remember(selectedFarm, readings, range) {
        val base = selectedFarm?.let { f -> readings.filter { it.farmId == f.id } } ?: readings
        val cutoff = range.windowMs?.let { nowMs - it }
        (if (cutoff != null) base.filter { it.recordedEpochMs >= cutoff } else base)
            .sortedBy { it.recordedEpochMs }
    }
    val farmAlerts = remember(selectedFarm, alerts) {
        selectedFarm?.let { f -> alerts.filter { it.farmId == f.id } } ?: alerts
    }
    val farmScans = remember(selectedFarm, scans) {
        selectedFarm?.let { f -> scans.filter { it.farmId == f.id } } ?: scans
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("farm_analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateRangeFilter.entries.forEach { option ->
                    FilterChip(
                        selected = range == option,
                        onClick = { range = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedFarm?.name ?: "All Accessible Farms",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Readings: ${farmReadings.size} • Alerts: ${farmAlerts.size} • AI Scans: ${farmScans.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (farmReadings.isNotEmpty()) {
                        val avgMoisture = farmReadings.map { it.soilMoisturePct }.average()
                        val avgTemp = farmReadings.map { it.airTempC }.average()
                        val totalRain = farmReadings.sumOf { it.rainfallMm }
                        Text(
                            text = "Avg Moisture: ${"%.1f".format(avgMoisture)}% • Avg Air Temp: ${"%.1f".format(avgTemp)}°C • Total Rainfall: ${"%.1f".format(totalRain)} mm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (farmReadings.isEmpty() && farmAlerts.isEmpty() && farmScans.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Analytics,
                    title = "Empty Analytics Dataset",
                    description = "No sensor logs, alerts, or crop scans match the selected farm and date filter."
                )
            }
        } else {
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
                    title = "Temperature Trend",
                    unit = "°C",
                    points = farmReadings.map { it.recordedEpochMs to it.airTempC },
                    lineColor = HarvestAmber
                )
            }
            item {
                TelemetryChartCard(
                    title = "Humidity Trend",
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
}
