package com.example

import com.example.database.AlertEntity
import com.example.database.CropScanEntity
import com.example.database.CropZoneEntity
import com.example.database.Esp32DeviceEntity
import com.example.database.FarmEntity
import com.example.database.SensorReadingEntity
import com.example.domain.EnvironmentalRiskEvaluator
import com.example.domain.IrrigationAdvisorEngine
import com.example.domain.RolePermissionGuard
import com.example.domain.SensorReadingValidator
import com.example.models.AlertSeverity
import com.example.models.AlertType
import com.example.models.CropScanRecord
import com.example.models.CropType
import com.example.models.CropZone
import com.example.models.DeviceConnectionStatus
import com.example.models.DiagnosisSeverity
import com.example.models.Esp32Device
import com.example.models.Farm
import com.example.models.FarmAlert
import com.example.models.GrowthStage
import com.example.models.RiskLevel
import com.example.models.SensorReading
import com.example.models.TelemetrySource
import com.example.models.UserProfile
import com.example.models.UserRole
import com.example.models.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgroGuardDomainUnitTest {

    private val nowMs = 1_750_000_000_000L

    private fun sampleZone(
        crop: CropType = CropType.PADDY,
        stage: GrowthStage = GrowthStage.VEGETATIVE
    ) = CropZone(
        id = "zone-1",
        farmId = "farm-1",
        ownerId = "farmer-1",
        sharedExpertIds = listOf("expert-1"),
        name = "North Plot A",
        cropType = crop.name,
        variety = "ADT-43",
        plantingDateEpochMs = nowMs - 20 * 86_400_000L,
        growthStage = stage.name,
        areaAcres = 2.0,
        minSoilMoisturePct = 55.0,
        maxSoilMoisturePct = 85.0,
        maxAirTempC = 37.0
    )

    private fun sampleReading(
        moisture: Double = 42.0,
        rainfallMm: Double = 0.0,
        airTempC: Double = 33.0,
        humidityPct: Double = 60.0,
        recordedEpochMs: Long = nowMs - 10 * 60_000L
    ) = SensorReading(
        id = "read-1",
        farmId = "farm-1",
        cropZoneId = "zone-1",
        deviceId = "esp32-01",
        ownerId = "farmer-1",
        source = TelemetrySource.ESP32_VERIFIED.name,
        soilMoisturePct = moisture,
        soilTempC = 27.0,
        airTempC = airTempC,
        humidityPct = humidityPct,
        lightLux = 35_000.0,
        rainfallMm = rainfallMm,
        waterLevelCm = 3.0,
        recordedEpochMs = recordedEpochMs
    )

    @Test
    fun `irrigation engine returns no actuation and empty state when no readings exist`() {
        val rec = IrrigationAdvisorEngine.recommend(
            zone = sampleZone(),
            recentReadings = emptyList(),
            weather = null,
            nowEpochMs = nowMs
        )
        assertFalse(rec.irrigationNeeded)
        assertTrue(rec.actionSummaryEn.contains("Insufficient sensor data"))
        assertTrue(rec.limitationsNote.contains("Automatic pump control is disabled"))
    }

    @Test
    fun `irrigation engine recommends watering when soil moisture is below threshold`() {
        val rec = IrrigationAdvisorEngine.recommend(
            zone = sampleZone(),
            recentReadings = listOf(sampleReading(moisture = 38.0)),
            weather = null,
            nowEpochMs = nowMs
        )
        assertTrue(rec.irrigationNeeded)
        assertTrue(rec.actionSummaryEn.contains("irrigation recommended", ignoreCase = true))
        assertTrue(rec.dataFreshnessNote.contains("Recent reading"))
    }

    @Test
    fun `irrigation engine defers irrigation when significant rainfall occurred`() {
        val rec = IrrigationAdvisorEngine.recommend(
            zone = sampleZone(),
            recentReadings = listOf(sampleReading(moisture = 40.0, rainfallMm = 18.5)),
            weather = null,
            nowEpochMs = nowMs
        )
        assertFalse(rec.irrigationNeeded)
        assertTrue(rec.reasoningPoints.any { it.contains("defer irrigation", ignoreCase = true) })
    }

    @Test
    fun `irrigation engine flags stale data older than 6 hours`() {
        val rec = IrrigationAdvisorEngine.recommend(
            zone = sampleZone(stage = GrowthStage.HARVEST_READY),
            recentReadings = listOf(
                sampleReading(moisture = 60.0, recordedEpochMs = nowMs - 8 * 3_600_000L)
            ),
            weather = null,
            nowEpochMs = nowMs
        )
        assertTrue(rec.dataFreshnessNote.contains("Stale reading"))
        assertTrue(rec.confidenceNote.contains("over 6 hours old"))
    }

    @Test
    fun `sensor validator accepts normal reading and rejects future timestamps or out of bounds`() {
        val validResult = SensorReadingValidator.validate(sampleReading(), nowEpochMs = nowMs)
        assertTrue(validResult.valid)
        assertTrue(validResult.errors.isEmpty())

        val futureReading = sampleReading(recordedEpochMs = nowMs + 15 * 60_000L)
        val futureResult = SensorReadingValidator.validate(futureReading, nowEpochMs = nowMs)
        assertFalse(futureResult.valid)
        assertTrue(futureResult.errors.any { it.contains("future") })

        val badMoisture = sampleReading(moisture = 140.0, airTempC = 80.0)
        val badResult = SensorReadingValidator.validate(badMoisture, nowEpochMs = nowMs)
        assertFalse(badResult.valid)
        assertTrue(badResult.errors.size >= 2)
    }

    @Test
    fun `sensor validator warns on extreme rail moisture and thermal spikes`() {
        val extreme = sampleReading(moisture = 0.0, airTempC = 47.0)
        val result = SensorReadingValidator.validate(extreme, nowEpochMs = nowMs)
        assertTrue(result.valid)
        assertTrue(result.warnings.size >= 2)
    }

    @Test
    fun `role permission guard enforces farmer expert admin boundaries and esp32 security`() {
        val farmer = UserProfile(userId = "farmer-1", role = UserRole.FARMER.name)
        val otherFarmer = UserProfile(userId = "farmer-2", role = UserRole.FARMER.name)
        val expert = UserProfile(userId = "expert-1", role = UserRole.EXPERT.name)
        val admin = UserProfile(userId = "admin-1", role = UserRole.ADMIN.name)

        val farm = Farm(id = "farm-1", ownerId = "farmer-1", sharedExpertIds = listOf("expert-1"))
        val scan = CropScanRecord(id = "scan-1", farmId = "farm-1", ownerId = "farmer-1", sharedExpertIds = listOf("expert-1"))

        assertTrue(RolePermissionGuard.canCreateFarm(farmer))
        assertFalse(RolePermissionGuard.canCreateFarm(expert))
        assertTrue(RolePermissionGuard.canAccessFarm(farmer, farm))
        assertFalse(RolePermissionGuard.canAccessFarm(otherFarmer, farm))
        assertTrue(RolePermissionGuard.canAccessFarm(expert, farm))
        assertTrue(RolePermissionGuard.canAccessFarm(admin, farm))
        assertFalse(RolePermissionGuard.canEditFarm(expert, farm))

        assertTrue(RolePermissionGuard.canVerifyScan(expert, scan))
        assertFalse(RolePermissionGuard.canVerifyScan(farmer, scan))
        assertTrue(RolePermissionGuard.canAccessAdminManagement(admin))
        assertFalse(RolePermissionGuard.canAccessAdminManagement(farmer))

        assertTrue(RolePermissionGuard.canClientSubmitTelemetrySource(TelemetrySource.MANUAL_LOG))
        assertFalse(RolePermissionGuard.canClientSubmitTelemetrySource(TelemetrySource.ESP32_VERIFIED))
    }

    @Test
    fun `environmental risk evaluator never fabricates risk levels when weather is unavailable`() {
        val unavailable = WeatherSnapshot(
            available = false,
            providerName = "Disabled",
            locationLabel = "Thanjavur",
            unavailableReason = "Disabled in settings"
        )
        val assessment = EnvironmentalRiskEvaluator.evaluate(CropType.BANANA, unavailable, null)
        assertEquals(4, assessment.risks.size)
        assertTrue(assessment.risks.all { it.level == RiskLevel.UNAVAILABLE })
    }

    @Test
    fun `room entity mappers round-trip domain models faithfully`() {
        val farm = Farm(
            id = "f1",
            ownerId = "u1",
            name = "Kaveri Delta",
            locationName = "Thanjavur",
            latitude = 10.78,
            longitude = 79.13,
            areaAcres = 3.5,
            primaryCrop = CropType.PADDY.name,
            sharedExpertIds = listOf("exp1", "exp2")
        )
        assertEquals(farm.name, FarmEntity.fromDomain(farm).toDomain().name)
        assertEquals(listOf("exp1", "exp2"), FarmEntity.fromDomain(farm).toDomain().sharedExpertIds)

        val zone = sampleZone()
        assertEquals(zone.variety, CropZoneEntity.fromDomain(zone).toDomain().variety)

        val device = Esp32Device(
            id = "esp-1",
            farmId = "f1",
            cropZoneId = "z1",
            ownerId = "u1",
            label = "Node A",
            connectionStatus = DeviceConnectionStatus.ONLINE.name,
            lastSeenEpochMs = nowMs
        )
        assertEquals(device.label, Esp32DeviceEntity.fromDomain(device).toDomain().label)

        val reading = sampleReading()
        assertEquals(reading.soilMoisturePct, SensorReadingEntity.fromDomain(reading).toDomain().soilMoisturePct, 0.001)

        val scan = CropScanRecord(
            id = "s1",
            farmId = "f1",
            cropZoneId = "z1",
            ownerId = "u1",
            cropType = CropType.TOMATO.name,
            possibleCondition = "Early Blight",
            visibleSymptoms = listOf("Concentric rings", "Yellow halo"),
            confidencePct = 74L,
            severity = DiagnosisSeverity.MODERATE.name,
            recommendedNextSteps = listOf("Consult agronomist", "Remove lower blighted leaves"),
            uncertaintyNotes = "Preliminary only"
        )
        val roundTripScan = CropScanEntity.fromDomain(scan).toDomain()
        assertEquals(scan.visibleSymptoms, roundTripScan.visibleSymptoms)
        assertEquals(scan.recommendedNextSteps, roundTripScan.recommendedNextSteps)

        val alert = FarmAlert(
            id = "a1",
            farmId = "f1",
            cropZoneId = "z1",
            ownerId = "u1",
            alertType = AlertType.HIGH_TEMP.name,
            severity = AlertSeverity.CRITICAL.name,
            title = "Heat Alert",
            explanation = "Temp > 38C"
        )
        assertEquals(alert.title, AlertEntity.fromDomain(alert).toDomain().title)
    }
}
