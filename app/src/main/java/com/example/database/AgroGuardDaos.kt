package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms ORDER BY name ASC")
    fun observeFarms(): Flow<List<FarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(farms: List<FarmEntity>)

    @Query("DELETE FROM farms")
    suspend fun clearAll()
}

@Dao
interface CropZoneDao {
    @Query("SELECT * FROM crop_zones ORDER BY name ASC")
    fun observeAllZones(): Flow<List<CropZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(zones: List<CropZoneEntity>)

    @Query("DELETE FROM crop_zones")
    suspend fun clearAll()
}

@Dao
interface Esp32DeviceDao {
    @Query("SELECT * FROM esp32_devices ORDER BY lastSeenEpochMs DESC")
    fun observeAllDevices(): Flow<List<Esp32DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<Esp32DeviceEntity>)

    @Query("DELETE FROM esp32_devices")
    suspend fun clearAll()
}

@Dao
interface SensorReadingDao {
    @Query("SELECT * FROM sensor_readings ORDER BY recordedEpochMs DESC")
    fun observeAllReadings(): Flow<List<SensorReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(readings: List<SensorReadingEntity>)

    @Query("DELETE FROM sensor_readings")
    suspend fun clearAll()
}

@Dao
interface CropScanDao {
    @Query("SELECT * FROM crop_scans ORDER BY cachedAtEpochMs DESC")
    fun observeAllScans(): Flow<List<CropScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(scans: List<CropScanEntity>)

    @Query("DELETE FROM crop_scans")
    suspend fun clearAll()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY cachedAtEpochMs DESC")
    fun observeAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(alerts: List<AlertEntity>)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync_queue ORDER BY createdAtEpochMs ASC")
    fun observePending(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync_queue ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingNow(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: PendingSyncEntity)

    @Query("DELETE FROM pending_sync_queue WHERE queueId = :queueId")
    suspend fun deleteById(queueId: Long)

    @Query("DELETE FROM pending_sync_queue")
    suspend fun clearAll()
}
