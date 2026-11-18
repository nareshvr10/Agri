package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "current") val current: OpenMeteoCurrent? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double? = null,
    @Json(name = "rain") val rain: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,rain,wind_speed_10m",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
