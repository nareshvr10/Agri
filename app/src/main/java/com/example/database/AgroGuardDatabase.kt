package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FarmEntity::class,
        CropZoneEntity::class,
        Esp32DeviceEntity::class,
        SensorReadingEntity::class,
        CropScanEntity::class,
        AlertEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AgroGuardDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao
    abstract fun cropZoneDao(): CropZoneDao
    abstract fun esp32DeviceDao(): Esp32DeviceDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun cropScanDao(): CropScanDao
    abstract fun alertDao(): AlertDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var INSTANCE: AgroGuardDatabase? = null

        fun getInstance(context: Context): AgroGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgroGuardDatabase::class.java,
                    "agroguard_offline.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
