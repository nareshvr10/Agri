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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.models.AppLanguage
import com.example.models.CropZone
import com.example.models.Farm
import com.example.models.IrrigationRecommendation
import com.example.models.RiskLevel
import com.example.models.UserRole
import com.example.models.WeatherRiskAssessment
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.cropLabel
import com.example.ui.components.formatEpochShort
import com.example.ui.components.stageLabel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.HarvestAmberContainer

@Composable
fun IrrigationRecommendationsScreen(
    selectedFarm: Farm?,
    zones: List<CropZone>,
    recommendations: List<IrrigationRecommendation>,
    userRole: UserRole,
    language: AppLanguage,
    isOnline: Boolean,
    onUpdateThresholds: (zoneId: String, minM: Double, maxM: Double, maxT: Double, isOnline: Boolean) -> Unit
) {
    var editingZone by remember { mutableStateOf<CropZone?>(null) }
    var minMText by remember { mutableStateOf("") }
    var maxMText by remember { mutableStateOf("") }
    var maxTText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("irrigation_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(color = HarvestAmberContainer, shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = stringResource(R.string.irrigation_no_actuation_note),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (selectedFarm == null || zones.none { it.farmId == selectedFarm.id }) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.WaterDrop,
                    title = "No Crop Zones Configured",
                    description = "Add a farm and crop zone (Paddy, Banana, or Tomato) to receive stage-specific irrigation recommendations."
                )
            }
        } else {
            items(recommendations, key = { it.zoneId }) { rec ->
                val matchingZone = zones.firstOrNull { it.id == rec.zoneId }
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
                            Text(
                                text = "${rec.zoneName} • ${cropLabel(rec.cropType, language)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (rec.irrigationNeeded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (rec.irrigationNeeded) "Irrigate" else "Hold",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (language == AppLanguage.TAMIL) rec.actionSummaryTa else rec.actionSummaryEn,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (language == AppLanguage.TAMIL) rec.estimatedVolumeNoteTa else rec.estimatedVolumeNoteEn,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text("Reasoning:", style = MaterialTheme.typography.labelLarge)
                        rec.reasoningPoints.forEach { point ->
                            Text("• $point", style = MaterialTheme.typography.bodyMedium)
                        }

                        Text(
                            text = "${rec.dataFreshnessNote} • ${rec.confidenceNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${stageLabel(rec.growthStage, language)} • ${rec.limitationsNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (matchingZone != null && userRole != UserRole.EXPERT) {
                            OutlinedButton(onClick = {
                                editingZone = matchingZone
                                minMText = matchingZone.minSoilMoisturePct.toInt().toString()
                                maxMText = matchingZone.maxSoilMoisturePct.toInt().toString()
                                maxTText = matchingZone.maxAirTempC.toInt().toString()
                            }) {
                                Icon(Icons.Default.Tune, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Configure Thresholds")
                            }
                        }

                        if (editingZone?.id == rec.zoneId && matchingZone != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(minMText, { minMText = it }, label = { Text("Min %") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(maxMText, { maxMText = it }, label = { Text("Max %") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(maxTText, { maxTText = it }, label = { Text("Max °C") }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val minM = minMText.toDoubleOrNull() ?: matchingZone.minSoilMoisturePct
                                    val maxM = maxMText.toDoubleOrNull() ?: matchingZone.maxSoilMoisturePct
                                    val maxT = maxTText.toDoubleOrNull() ?: matchingZone.maxAirTempC
                                    onUpdateThresholds(matchingZone.id, minM, maxM, maxT, isOnline)
                                    editingZone = null
                                }) {
                                    Text(stringResource(R.string.action_save))
                                }
                                OutlinedButton(onClick = { editingZone = null }) {
                                    Text(stringResource(R.string.action_cancel))
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
fun WeatherAndRiskScreen(
    selectedFarm: Farm?,
    assessment: WeatherRiskAssessment,
    language: AppLanguage,
    onRefreshWeather: () -> Unit
) {
    val weather = assessment.weather

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weather_risk_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFarm?.locationName ?: "Farm Weather",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = onRefreshWeather) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_refresh))
                }
            }
        }

        item {
            if (!weather.available) {
                EmptyStateCard(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.status_weather_unavailable),
                    description = weather.unavailableReason
                        ?: "Live weather provider data is unavailable. No fabricated forecast will be displayed."
                )
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("${weather.providerName} • ${weather.locationLabel}", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Temp: ${weather.temperatureC?.let { "${"%.1f".format(it)} °C" } ?: "—"} • Humidity: ${weather.humidityPct?.let { "${"%.0f".format(it)}%" } ?: "—"}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Rainfall: ${weather.rainfallMm?.let { "${"%.1f".format(it)} mm" } ?: "—"} • Wind: ${weather.windSpeedKmh?.let { "${"%.1f".format(it)} km/h" } ?: "—"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Fetched: ${formatEpochShort(weather.fetchedAtEpochMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text("Environmental Risk Categories", style = MaterialTheme.typography.titleMedium)
        }

        items(assessment.risks, key = { it.category.name }) { risk ->
            val (badgeBg, badgeFg) = when (risk.level) {
                RiskLevel.HIGH -> AlertCrimsonContainer to AlertCrimson
                RiskLevel.MODERATE -> HarvestAmberContainer to Color(0xFF78350F)
                RiskLevel.LOW -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                RiskLevel.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.TAMIL) risk.titleTa else risk.titleEn,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                            Text(
                                text = risk.level.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = risk.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
