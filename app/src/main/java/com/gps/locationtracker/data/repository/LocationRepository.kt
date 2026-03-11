package com.gps.locationtracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.gps.locationtracker.data.models.LocationData
import com.gps.locationtracker.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Room Database
@Database(entities = [LocationData::class], version = 1, exportSchema = false)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: LocationDatabase? = null

        fun getDatabase(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Room DAO
@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(location: LocationData): Long

    @Update
    suspend fun updateLocation(location: LocationData)

    @Delete
    suspend fun deleteLocation(location: LocationData)

    @Query("DELETE FROM location_data WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM location_data WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM location_data ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationData>>

    @Query("SELECT * FROM location_data WHERE id = :id")
    suspend fun getLocationById(id: Int): LocationData?

    @Query("SELECT * FROM location_data WHERE uploadStatus = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingLocations(): Flow<List<LocationData>>

    @Query("SELECT * FROM location_data WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getLocationsByDateRange(startTime: Long, endTime: Long): Flow<List<LocationData>>

    @Query("SELECT * FROM location_data WHERE syncedToCloud = 0 ORDER BY timestamp ASC")
    fun getUnsyncedLocations(): Flow<List<LocationData>>

    @Query("SELECT COUNT(*) FROM location_data")
    suspend fun getTotalLocationsCount(): Int

    @Query("DELETE FROM location_data WHERE timestamp < :timestamp")
    suspend fun deleteLocationsOlderThan(timestamp: Long)

    @Query("UPDATE location_data SET uploadStatus = :status, syncedToCloud = :synced WHERE id = :id")
    suspend fun updateLocationStatus(id: Int, status: String, synced: Boolean)

    @Query("SELECT * FROM location_data WHERE source = :source ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLocationBySource(source: String): LocationData?

    @Query("SELECT COUNT(*) FROM location_data WHERE syncedToCloud = 1")
    suspend fun getSyncedLocationsCount(): Int
}

// Location Repository
class LocationRepository(private val context: Context) {

    private val locationDatabase = LocationDatabase.getDatabase(context)
    private val locationDao = locationDatabase.locationDao()
    private val dataStore: DataStore<Preferences> = context.locationDataStore
    private val sdf = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.US)

    // Insert new location
    suspend fun insertLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        accuracy: Float = 0f,
        source: String = "AUTO"
    ): Long {
        return try {
            val timestamp = System.currentTimeMillis()
            val formattedTime = sdf.format(Date(timestamp))

            val locationData = LocationData(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                accuracy = accuracy,
                timestamp = timestamp,
                formattedTime = formattedTime,
                source = source,
                uploadStatus = "PENDING"
            )

            Timber.d("Inserting location: $locationData")
            locationDao.insertLocation(locationData)
        } catch (e: Exception) {
            Timber.e("Error inserting location: ${e.message}")
            -1L
        }
    }

    // Get all locations
    fun getAllLocations(): Flow<List<LocationData>> {
        return locationDao.getAllLocations()
    }

    // Get pending locations (not uploaded)
    fun getPendingLocations(): Flow<List<LocationData>> {
        return locationDao.getPendingLocations()
    }

    // Get unsynced locations
    fun getUnsyncedLocations(): Flow<List<LocationData>> {
        return locationDao.getUnsyncedLocations()
    }

    // Update location status
    suspend fun updateLocationStatus(id: Int, status: String, synced: Boolean = false) {
        try {
            locationDao.updateLocationStatus(id, status, synced)
            Timber.d("Updated location $id status to $status, synced: $synced")
        } catch (e: Exception) {
            Timber.e("Error updating location status: ${e.message}")
        }
    }

    // Get locations by date range
    fun getLocationsByDateRange(startTime: Long, endTime: Long): Flow<List<LocationData>> {
        return locationDao.getLocationsByDateRange(startTime, endTime)
    }

    // Get last location
    fun getLastLocationBySource(source: String): Flow<LocationData?> {
        return kotlinx.coroutines.flow.flow {
            val location = locationDao.getLastLocationBySource(source)
            emit(location)
        }
    }

    // Save to offline file
    suspend fun saveToOfflineFile(locationData: LocationData) {
        try {
            val offlineData = context.openFileOutput(
                Constants.OFFLINE_LOCATION_FILE,
                Context.MODE_APPEND
            )
            val line = "${locationData.latitude},${locationData.longitude}," +
                    "${locationData.timestamp},${locationData.formattedTime}," +
                    "${locationData.source}\n"
            offlineData.write(line.toByteArray())
            offlineData.close()
            Timber.d("Saved location to offline file")
        } catch (e: Exception) {
            Timber.e("Error saving to offline file: ${e.message}")
        }
    }

    // Read offline file
    suspend fun readOfflineFile(): List<String> {
        return try {
            val file = context.getFileStreamPath(Constants.OFFLINE_LOCATION_FILE)
            if (file.exists()) {
                file.readLines()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e("Error reading offline file: ${e.message}")
            emptyList()
        }
    }

    // Clear offline file
    suspend fun clearOfflineFile() {
        try {
            context.deleteFile(Constants.OFFLINE_LOCATION_FILE)
            Timber.d("Cleared offline file")
        } catch (e: Exception) {
            Timber.e("Error clearing offline file: ${e.message}")
        }
    }

    // Delete old locations
    suspend fun deleteLocationsOlderThan(days: Int) {
        try {
            val timestamp = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
            locationDao.deleteLocationsOlderThan(timestamp)
            Timber.d("Deleted locations older than $days days")
        } catch (e: Exception) {
            Timber.e("Error deleting old locations: ${e.message}")
        }
    }

    suspend fun deleteLocation(id: Int) {
        try {
            locationDao.deleteById(id)
            Timber.d("Deleted location with ID: $id")
        } catch (e: Exception) {
            Timber.e("Error deleting location $id: ${e.message}")
        }
    }

    suspend fun deleteMultipleLocations(ids: List<Int>) {
        try {
            locationDao.deleteByIds(ids)
            Timber.d("Deleted ${ids.size} locations")
        } catch (e: Exception) {
            Timber.e("Error deleting multiple locations: ${e.message}")
        }
    }

    // Get statistics
    suspend fun getLocationCount(): Int {
        return try {
            locationDao.getTotalLocationsCount()
        } catch (e: Exception) {
            Timber.e("Error getting location count: ${e.message}")
            0
        }
    }

    suspend fun getSyncedCount(): Int {
        return try {
            locationDao.getSyncedLocationsCount()
        } catch (e: Exception) {
            Timber.e("Error getting synced count: ${e.message}")
            0
        }
    }

    // Export locations as CSV
    suspend fun exportLocationsAsCsv(): String {
        return try {
            val locations = locationDao.getAllLocations().first()
            val csv = StringBuilder()
            csv.append("Latitude,Longitude,Altitude,Accuracy,Timestamp,FormattedTime,Source,SyncStatus\n")

            locations.forEach { location ->
                csv.append("${location.latitude},${location.longitude},${location.altitude}," +
                        "${location.accuracy},${location.timestamp},${location.formattedTime}," +
                        "${location.source},${location.uploadStatus}\n")
            }

            Timber.d("Exported ${locations.size} locations as CSV")
            csv.toString()
        } catch (e: Exception) {
            Timber.e("Error exporting locations: ${e.message}")
            ""
        }
    }
}

// DataStore extension - Renamed to avoid conflict
val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore(name = "location_settings")
