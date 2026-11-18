package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.models.AiScanAnalysisResult
import com.example.models.BackendMode
import com.example.models.BackendSettings
import com.example.models.CropType
import com.example.models.DiagnosisSeverity
import com.example.models.WeatherProviderMode
import com.example.models.WeatherSnapshot
import com.example.network.BackendClientFactory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class FirebaseAiDiagnosisRepository : AiDiagnosisRepository {

    private val schema = Schema.obj(
        properties = mapOf(
            "identifiedCrop" to Schema.string(description = "Identified crop name or Unknown if unclear"),
            "possibleCondition" to Schema.string(description = "Preliminary possible disease, pest, nutrient stress, or Healthy appearance"),
            "visibleSymptoms" to Schema.array(
                items = Schema.string(),
                description = "Observable leaf/plant visual symptoms from the image"
            ),
            "confidencePct" to Schema.integer(description = "Estimated screening confidence percentage from 0 to 100"),
            "severity" to Schema.enumeration(
                values = listOf("LOW", "MODERATE", "HIGH", "CRITICAL", "UNKNOWN"),
                description = "Preliminary visual severity"
            ),
            "recommendedNextSteps" to Schema.array(
                items = Schema.string(),
                description = "Safe preliminary agronomic checks and advice to consult a local agricultural officer"
            ),
            "uncertaintyNotes" to Schema.string(description = "Explicit statement of diagnostic uncertainty, image limitations, and need for expert confirmation")
        )
    )

    private val model by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    override suspend fun analyzeCropImage(
        cropType: CropType,
        bitmap: Bitmap,
        settings: BackendSettings
    ): Result<AiScanAnalysisResult> {
        if (settings.backendMode == BackendMode.CUSTOM_FASTAPI) {
            if (settings.fastApiBaseUrl.isBlank()) {
                return Result.success(
                    AiScanAnalysisResult(
                        identifiedCrop = cropType.labelEn,
                        possibleCondition = "AI service not configured",
                        visibleSymptoms = emptyList(),
                        confidencePct = 0,
                        severity = DiagnosisSeverity.UNKNOWN,
                        recommendedNextSteps = listOf("Configure a valid FastAPI backend URL or switch to Firebase Cloud mode in Settings."),
                        uncertaintyNotes = "AI service not configured — no remote inference performed.",
                        isServiceAvailable = false
                    )
                )
            }
            return Result.failure(
                IllegalStateException("Custom FastAPI AI endpoint requires an active authenticated backend deployment at ${settings.fastApiBaseUrl}.")
            )
        }

        val scaledBitmap = scaleBitmapForUpload(bitmap, maxDim = 1024)
        val prompt = """
            You are an agricultural crop screening assistant for ${cropType.labelEn}.
            Examine the provided leaf/plant image carefully.
            Rules:
            1. Never claim diagnostic certainty. Always describe findings as preliminary visual observations.
            2. If the image is blurry, dark, or does not clearly show a crop leaf, say so in uncertaintyNotes and set severity to UNKNOWN with low confidencePct.
            3. Always advise confirming with a qualified Agricultural Expert before chemical or pesticide intervention.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(scaledBitmap)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI model returned an empty response.")
            val json = JSONObject(jsonText)
            val symptomsArr = json.optJSONArray("visibleSymptoms")
            val symptoms = List(symptomsArr?.length() ?: 0) { idx ->
                symptomsArr?.optString(idx).orEmpty()
            }.filter { it.isNotBlank() }

            val stepsArr = json.optJSONArray("recommendedNextSteps")
            val steps = List(stepsArr?.length() ?: 0) { idx ->
                stepsArr?.optString(idx).orEmpty()
            }.filter { it.isNotBlank() }

            Result.success(
                AiScanAnalysisResult(
                    identifiedCrop = json.optString("identifiedCrop", cropType.labelEn),
                    possibleCondition = json.optString("possibleCondition", "Unclear preliminary observation"),
                    visibleSymptoms = symptoms,
                    confidencePct = json.optInt("confidencePct", 0).coerceIn(0, 100),
                    severity = DiagnosisSeverity.fromRaw(json.optString("severity")),
                    recommendedNextSteps = steps,
                    uncertaintyNotes = json.optString(
                        "uncertaintyNotes",
                        "Preliminary AI screening only; confirm with an agricultural expert."
                    ),
                    isServiceAvailable = true
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI crop scan timed out", e)
            Result.failure(IllegalStateException("AI analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI crop scan failed", e)
            Result.failure(e)
        }
    }

    private fun scaleBitmapForUpload(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private companion object {
        const val TAG = "AiDiagnosisRepo"
    }
}

class LiveWeatherRepository : WeatherRepository {
    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        settings: BackendSettings
    ): WeatherSnapshot {
        if (settings.weatherProviderMode == WeatherProviderMode.DISABLED) {
            return WeatherSnapshot(
                available = false,
                providerName = "Disabled",
                locationLabel = locationLabel,
                unavailableReason = "Weather provider is disabled in Settings."
            )
        }
        if (latitude == 0.0 && longitude == 0.0) {
            return WeatherSnapshot(
                available = false,
                providerName = settings.weatherProviderMode.name,
                locationLabel = locationLabel,
                unavailableReason = "Farm coordinates (latitude/longitude) are not set."
            )
        }
        return try {
            when (settings.weatherProviderMode) {
                WeatherProviderMode.OPEN_METEO_LIVE -> {
                    val response = BackendClientFactory.openMeteoApi.getCurrentWeather(latitude, longitude)
                    val cur = response.current
                    if (cur == null) {
                        WeatherSnapshot(
                            available = false,
                            providerName = "Open-Meteo",
                            locationLabel = locationLabel,
                            unavailableReason = "Live weather provider returned no current observations."
                        )
                    } else {
                        WeatherSnapshot(
                            available = true,
                            providerName = "Open-Meteo Live",
                            locationLabel = locationLabel,
                            temperatureC = cur.temperature2m,
                            humidityPct = cur.relativeHumidity2m,
                            rainfallMm = cur.rain,
                            windSpeedKmh = cur.windSpeed10m,
                            fetchedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                }
                WeatherProviderMode.CUSTOM_PROXY -> {
                    val baseUrl = settings.customWeatherUrl.trim()
                    if (baseUrl.isBlank()) {
                        WeatherSnapshot(
                            available = false,
                            providerName = "Custom Proxy",
                            locationLabel = locationLabel,
                            unavailableReason = "Custom weather proxy URL is not configured in Settings."
                        )
                    } else {
                        val dto = BackendClientFactory.createBackendApi(baseUrl)
                            .getWeatherProxy(latitude, longitude)
                        WeatherSnapshot(
                            available = true,
                            providerName = dto.providerName,
                            locationLabel = locationLabel,
                            temperatureC = dto.temperatureC,
                            humidityPct = dto.humidityPct,
                            rainfallMm = dto.rainfallMm,
                            windSpeedKmh = dto.windSpeedKmh,
                            fetchedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                }
                WeatherProviderMode.DISABLED -> error("Handled above")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WeatherSnapshot(
                available = false,
                providerName = settings.weatherProviderMode.name,
                locationLabel = locationLabel,
                unavailableReason = e.localizedMessage ?: "Unable to reach weather provider."
            )
        }
    }
}
