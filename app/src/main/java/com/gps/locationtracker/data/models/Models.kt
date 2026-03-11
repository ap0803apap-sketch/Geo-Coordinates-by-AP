package com.gps.locationtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

// Location Data - Room Entity
@Entity(tableName = "location_data")
data class LocationData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long,
    val formattedTime: String,
    val source: String = "AUTO", // AUTO, SMS, MANUAL
    val uploadStatus: String = "PENDING", // PENDING, UPLOADING, UPLOADED, FAILED
    val driveFileId: String? = null,
    val syncedToCloud: Boolean = false
) : Serializable

// Schedule Time Data
data class ScheduleTime(
    val id: String = "",
    val hour: Int = 12,
    val minute: Int = 0,
    val isEnabled: Boolean = true,
    val name: String = "Schedule ${System.currentTimeMillis()}"
) : Serializable

// User Preferences
data class UserPreferences(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val profilePictureUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = false,
    val isSetupComplete: Boolean = false,
    val smsTriggerKey: String = "",
    val selectedGpsMethod: String = "FUSED_LOCATION",
    val scheduledTimes: List<ScheduleTime> = emptyList(),
    val isDarkMode: Boolean = false,
    val isAmoledMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val isDeviceAdminEnabled: Boolean = false,
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    val lastTimestamp: Long = 0L,
    val googleDriveToken: String? = null
) : Serializable

// Location Response Data
data class LocationResponse(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "FUSED"
)

// Drive File Info
data class DriveFileInfo(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val createdTime: Long,
    val modifiedTime: Long,
    val sizeBytes: Long
)

// Sync Status
data class SyncStatus(
    val totalLocations: Int = 0,
    val synced: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false
)

// App Statistics
data class AppStatistics(
    val totalLocationsTracked: Int = 0,
    val totalLocationsSynced: Int = 0,
    val appStartTime: Long = System.currentTimeMillis(),
    val lastTrackingTime: Long = 0L,
    val totalTrackingDuration: Long = 0L
)

// Error Information
data class ErrorInfo(
    val errorCode: Int,
    val errorMessage: String,
    val errorType: ErrorType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ErrorType {
    PERMISSION_DENIED,
    LOCATION_NOT_AVAILABLE,
    NETWORK_ERROR,
    DRIVE_UPLOAD_ERROR,
    DATABASE_ERROR,
    BIOMETRIC_ERROR,
    AUTHENTICATION_ERROR,
    UNKNOWN
}

// SMS Message
data class SMSMessage(
    val sender: String,
    val body: String,
    val timestamp: Long
)

// Device Admin Status
data class DeviceAdminStatus(
    val isEnabled: Boolean = false,
    val enabledTime: Long = 0L,
    val lastCheckTime: Long = System.currentTimeMillis()
)

// Notification Payload
data class NotificationPayload(
    val title: String,
    val message: String,
    val notificationId: Int,
    val channelId: String
)

// Location History Statistics
data class LocationHistoryStats(
    val totalLocations: Int = 0,
    val todayLocations: Int = 0,
    val thisWeekLocations: Int = 0,
    val thisMonthLocations: Int = 0,
    val averageAccuracy: Float = 0f,
    val lastLocationTime: Long = 0L
)
