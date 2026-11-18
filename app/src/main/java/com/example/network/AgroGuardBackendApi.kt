package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Replaceable FastAPI / custom backend API contract for AgroGuard AI.
 * Handles server-authenticated ESP32 ingestion, per-device token revocation,
 * AI crop screening proxy, and custom weather proxy integration.
 */
@JsonClass(generateAdapter = true)
data class Esp32TelemetryIngestContract(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "farm_id") val farmId: String,
    @Json(name = "crop_zone_id") val cropZoneId: String,
    @Json(name = "recorded_epoch_ms") val recordedEpochMs: Long,
    @Json(name = "soil_moisture_pct") val soilMoisturePct: Double,
    @Json(name = "soil_temp_c") val soilTempC: Double,
    @Json(name = "air_temp_c") val airTempC: Double,
    @Json(name = "humidity_pct") val humidityPct: Double,
    @Json(name = "light_lux") val lightLux: Double,
    @Json(name = "rainfall_mm") val rainfallMm: Double,
    @Json(name = "water_level_cm") val waterLevelCm: Double? = null
)

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "connection_status") val connectionStatus: String,
    @Json(name = "last_seen_epoch_ms") val lastSeenEpochMs: Long,
    @Json(name = "revoked") val revoked: Boolean
)

@JsonClass(generateAdapter = true)
data class CustomWeatherProxyDto(
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "temperature_c") val temperatureC: Double? = null,
    @Json(name = "humidity_pct") val humidityPct: Double? = null,
    @Json(name = "rainfall_mm") val rainfallMm: Double? = null,
    @Json(name = "wind_speed_kmh") val windSpeedKmh: Double? = null
)

@JsonClass(generateAdapter = true)
data class FastApiHealthResponse(
    @Json(name = "status") val status: String,
    @Json(name = "version") val version: String? = null
)

interface AgroGuardBackendApi {
    @GET("health")
    suspend fun healthCheck(): FastApiHealthResponse

    @GET("api/v1/devices/{deviceId}/status")
    suspend fun getDeviceStatus(
        @Header("Authorization") bearerToken: String,
        @Path("deviceId") deviceId: String
    ): DeviceStatusDto

    @POST("api/v1/devices/{deviceId}/revoke")
    suspend fun revokeDeviceToken(
        @Header("Authorization") bearerToken: String,
        @Path("deviceId") deviceId: String
    ): DeviceStatusDto

    @GET("api/v1/weather")
    suspend fun getWeatherProxy(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): CustomWeatherProxyDto

    @POST("api/v1/ai/crop-scan")
    suspend fun analyzeCropRemote(
        @Header("Authorization") bearerToken: String,
        @Body payload: Map<String, String>
    ): Map<String, Any>
}
