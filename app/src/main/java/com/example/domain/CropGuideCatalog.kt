package com.example.domain

import com.example.models.CropGuideSpec
import com.example.models.CropType
import com.example.models.GrowthStage
import com.example.models.SensorMetricSpec

object CropGuideCatalog {

    val sensorMetadata: List<SensorMetricSpec> = listOf(
        SensorMetricSpec("soil_moisture", "Soil Moisture", "மண் ஈரப்பதம்", "%", 0.0, 100.0),
        SensorMetricSpec("soil_temp", "Soil Temperature", "மண் வெப்பநிலை", "°C", -10.0, 70.0),
        SensorMetricSpec("air_temp", "Air Temperature", "காற்று வெப்பநிலை", "°C", -10.0, 65.0),
        SensorMetricSpec("humidity", "Relative Humidity", "ஈரப்பதம்", "%", 0.0, 100.0),
        SensorMetricSpec("light_lux", "Light Intensity", "ஒளி அளவு", "lux", 0.0, 200000.0),
        SensorMetricSpec("rainfall", "Rainfall", "மழை அளவு", "mm", 0.0, 1000.0),
        SensorMetricSpec("water_level", "Field Water Level", "வயல் நீர் மட்டம்", "cm", 0.0, 500.0)
    )

    val cropSpecs: Map<CropType, CropGuideSpec> = mapOf(
        CropType.PADDY to CropGuideSpec(
            cropType = CropType.PADDY,
            scientificName = "Oryza sativa",
            defaultMinMoisturePct = 55.0,
            defaultMaxMoisturePct = 85.0,
            defaultMaxAirTempC = 36.0,
            typicalVarieties = listOf("ADT 43", "Ponni", "CR 1009", "IR 20", "BPT 5204"),
            stageSummaryEn = "Maintain shallow standing water or saturated root-zone moisture during active vegetative and flowering phases; allow controlled drainage before harvest.",
            stageSummaryTa = "வளர்ச்சி மற்றும் பூக்கும் பருவத்தில் மண் ஈரப்பதம் அல்லது குறைந்த நீர் மட்டத்தை பராமரிக்கவும்; அறுவடைக்கு முன் நீரை வடிக்கவும்."
        ),
        CropType.BANANA to CropGuideSpec(
            cropType = CropType.BANANA,
            scientificName = "Musa spp.",
            defaultMinMoisturePct = 50.0,
            defaultMaxMoisturePct = 78.0,
            defaultMaxAirTempC = 35.0,
            typicalVarieties = listOf("Grand Naine (G9)", "Poovan", "Nendran", "Robusta", "Red Banana"),
            stageSummaryEn = "Requires consistent root-zone moisture with good drainage to avoid waterlogging; sensitive to prolonged dry spells during flowering and bunch development.",
            stageSummaryTa = "நீர் தேங்காத சமச்சீர் மண் ஈரப்பதம் தேவை; பூக்கும் மற்றும் குலை பிடிக்கும் பருவத்தில் வறட்சி தவிர்க்கப்பட வேண்டும்."
        ),
        CropType.TOMATO to CropGuideSpec(
            cropType = CropType.TOMATO,
            scientificName = "Solanum lycopersicum",
            defaultMinMoisturePct = 40.0,
            defaultMaxMoisturePct = 68.0,
            defaultMaxAirTempC = 34.0,
            typicalVarieties = listOf("PKM 1", "Arka Rakshak", "CO 3", "Shivam", "Pusa Ruby"),
            stageSummaryEn = "Needs steady moderate moisture and well-drained soil; excess humidity and overhead wetting can favour foliar fungal stress.",
            stageSummaryTa = "மிதமான சீரான மண் ஈரப்பதம் மற்றும் நல்ல வடிகால் தேவை; அதிக ஈரப்பதம் இலை நோய்களை அதிகரிக்கலாம்."
        )
    )

    fun specFor(cropType: CropType): CropGuideSpec =
        cropSpecs[cropType] ?: cropSpecs.getValue(CropType.PADDY)

    fun stageMoistureAdjustment(stage: GrowthStage): Double = when (stage) {
        GrowthStage.SEEDLING -> 3.0
        GrowthStage.VEGETATIVE -> 0.0
        GrowthStage.FLOWERING -> 5.0
        GrowthStage.FRUITING_MATURITY -> 2.0
        GrowthStage.HARVEST_READY -> -8.0
    }
}
