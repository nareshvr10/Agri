package com.example.domain

import com.example.models.CropZone
import com.example.models.GrowthStage
import com.example.models.IrrigationRecommendation
import com.example.models.SensorReading
import com.example.models.TelemetrySource
import com.example.models.WeatherSnapshot
import java.util.concurrent.TimeUnit

object IrrigationAdvisorEngine {

    private const val STALE_THRESHOLD_MS = 6 * 60 * 60 * 1000L // 6 hours

    fun recommend(
        zone: CropZone,
        recentReadings: List<SensorReading>,
        weather: WeatherSnapshot?,
        nowEpochMs: Long = System.currentTimeMillis()
    ): IrrigationRecommendation {
        val crop = zone.resolvedCrop()
        val stage = zone.resolvedStage()
        val latest = recentReadings.maxByOrNull { it.recordedEpochMs }

        if (latest == null) {
            return IrrigationRecommendation(
                zoneId = zone.id,
                zoneName = zone.name,
                cropType = crop,
                growthStage = stage,
                actionSummaryEn = "Insufficient sensor data — inspect field moisture manually",
                actionSummaryTa = "சென்சார் தரவு இல்லை — வயல் ஈரப்பதத்தை நேரில் சரிபார்க்கவும்",
                irrigationNeeded = false,
                estimatedVolumeNoteEn = "No automated estimate available without recent soil moisture data.",
                estimatedVolumeNoteTa = "மண் ஈரப்பத தரவு இல்லாமல் நீர்ப்பாசன அளவை கணக்கிட முடியாது.",
                reasoningPoints = listOf(
                    "No ESP32 or manual soil readings recorded for ${zone.name}.",
                    "Target moisture window for ${crop.labelEn} (${stage.labelEn}): ${zone.minSoilMoisturePct.toInt()}%–${zone.maxSoilMoisturePct.toInt()}%."
                ),
                confidenceNote = "Confidence: Low (no sensor readings available).",
                dataFreshnessNote = "No readings available.",
                limitationsNote = "Connect an ESP32 node or record a field observation. Automatic pump control is disabled.",
                sourceLabel = TelemetrySource.UNCONNECTED.labelEn
            )
        }

        val ageMs = (nowEpochMs - latest.recordedEpochMs).coerceAtLeast(0L)
        val ageMinutes = TimeUnit.MILLISECONDS.toMinutes(ageMs)
        val isStale = ageMs > STALE_THRESHOLD_MS
        val freshnessNote = when {
            ageMinutes < 5 -> "Fresh reading (${ageMinutes}m ago)"
            ageMinutes < 60 -> "Recent reading (${ageMinutes}m ago)"
            else -> {
                val hours = TimeUnit.MILLISECONDS.toHours(ageMs)
                if (isStale) "Stale reading (${hours}h ago — recheck field)" else "Reading from ${hours}h ago"
            }
        }

        val stageAdjustment = CropGuideCatalog.stageMoistureAdjustment(stage)
        val effectiveMin = (zone.minSoilMoisturePct + stageAdjustment).coerceIn(15.0, 90.0)
        val effectiveMax = (zone.maxSoilMoisturePct + stageAdjustment).coerceIn(effectiveMin + 5.0, 98.0)

        val recentRainfall = recentReadings
            .filter { nowEpochMs - it.recordedEpochMs <= 24 * 60 * 60 * 1000L }
            .sumOf { it.rainfallMm }
        val liveWeatherRain = weather?.takeIf { it.available }?.rainfallMm ?: 0.0
        val effectiveRain = maxOf(recentRainfall, liveWeatherRain)

        val highHeat = latest.airTempC >= zone.maxAirTempC ||
            (weather?.available == true && (weather.temperatureC ?: 0.0) >= zone.maxAirTempC)
        val lowHumidity = latest.humidityPct < 45.0

        val reasoning = mutableListOf<String>()
        reasoning += "Current soil moisture: ${"%.1f".format(latest.soilMoisturePct)}% (adjusted stage window: ${"%.0f".format(effectiveMin)}%–${"%.0f".format(effectiveMax)}%)."
        reasoning += "Air temperature: ${"%.1f".format(latest.airTempC)}°C, Humidity: ${"%.0f".format(latest.humidityPct)}%."
        if (effectiveRain > 0.0) {
            reasoning += "Recent/observed rainfall: ${"%.1f".format(effectiveRain)} mm."
        }
        if (stage == GrowthStage.HARVEST_READY) {
            reasoning += "Crop is in Harvest Ready stage — lower target moisture applies to avoid lodging or fruit splitting."
        }

        val deficit = effectiveMin - latest.soilMoisturePct
        val needed = when {
            effectiveRain >= 12.0 -> {
                reasoning += "Significant rainfall detected (${"%.1f".format(effectiveRain)} mm) — defer irrigation to prevent waterlogging."
                false
            }
            latest.soilMoisturePct < effectiveMin -> {
                reasoning += "Soil moisture is ${"%.1f".format(deficit)}% below the stage threshold."
                true
            }
            latest.soilMoisturePct <= effectiveMin + 4.0 && highHeat && lowHumidity -> {
                reasoning += "Moisture is near lower threshold during elevated heat and dry air; light supplemental irrigation advised."
                true
            }
            else -> {
                reasoning += "Soil moisture remains within the configured threshold band."
                false
            }
        }

        val (summaryEn, summaryTa, volumeEn, volumeTa) = if (needed) {
            val priority = if (deficit >= 12.0 || highHeat) "Priority" else "Scheduled"
            val estMinutes = ((deficit.coerceAtLeast(3.0) * 2.5)).toInt().coerceIn(10, 60)
            Quad(
                "$priority irrigation recommended for ${zone.name}",
                "${zone.name} பகுதிக்கு நீர்ப்பாசனம் பரிந்துரைக்கப்படுகிறது",
                "Check field channels/drip lines and irrigate for ~$estMinutes min until soil moisture approaches ${"%.0f".format((effectiveMin + effectiveMax) / 2.0)}%.",
                "சொட்டு நீர் அல்லது வாய்க்கால் வழியாக சுமார் $estMinutes நிமிடங்கள் பாசனம் செய்து ஈரப்பதத்தை ${"%.0f".format((effectiveMin + effectiveMax) / 2.0)}% வரை உயர்த்தவும்."
            )
        } else if (latest.soilMoisturePct > effectiveMax) {
            Quad(
                "Hold irrigation — excess soil moisture detected",
                "நீர்ப்பாசனம் வேண்டாம் — அதிக ஈரப்பதம் உள்ளது",
                "Ensure field drainage is clear; target upper bound is ${"%.0f".format(effectiveMax)}%.",
                "வயல் வடிகால் சரியாக உள்ளதா என சரிபார்க்கவும் (அதிகபட்ச வரம்பு ${"%.0f".format(effectiveMax)}%)."
            )
        } else {
            Quad(
                "No immediate irrigation required",
                "தற்போது நீர்ப்பாசனம் தேவையில்லை",
                "Moisture is sufficient for ${crop.labelEn} (${stage.labelEn}). Re-evaluate at next sensor interval.",
                "${crop.labelTa} பயிருக்கு தற்போதைய ஈரப்பதம் போதுமானது. அடுத்த அளவீட்டில் மீண்டும் சரிபார்க்கவும்."
            )
        }

        val confidence = when {
            isStale -> "Confidence: Low-Moderate (sensor data is over 6 hours old)."
            latest.resolvedSource() == TelemetrySource.MANUAL_LOG -> "Confidence: Moderate (based on manual field log; connect ESP32 for continuous trend)."
            else -> "Confidence: Good (based on verified sensor telemetry and crop stage thresholds)."
        }

        return IrrigationRecommendation(
            zoneId = zone.id,
            zoneName = zone.name,
            cropType = crop,
            growthStage = stage,
            actionSummaryEn = summaryEn,
            actionSummaryTa = summaryTa,
            irrigationNeeded = needed,
            estimatedVolumeNoteEn = volumeEn,
            estimatedVolumeNoteTa = volumeTa,
            reasoningPoints = reasoning,
            confidenceNote = confidence,
            dataFreshnessNote = freshnessNote,
            limitationsNote = "Advisory only — verify soil feel on-site before opening valves. Automatic pump switching is disabled.",
            sourceLabel = latest.resolvedSource().labelEn
        )
    }

    private data class Quad(
        val a: String,
        val b: String,
        val c: String,
        val d: String
    )
}
