package com.gps.locationtracker.utils

object Constants {
    // Application
    const val APP_NAME = "GPS Location Tracker"
    const val APP_VERSION = "1.0.0"

    // Preferences
    const val PREFS_NAME = "gps_tracker_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_PROFILE_PICTURE = "user_profile_picture"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_IS_GUEST = "is_guest"
    const val KEY_IS_SETUP_COMPLETE = "is_setup_complete"
    const val KEY_TRACKING_ENABLED = "tracking_enabled"
    const val KEY_SMS_TRIGGER_KEY = "sms_trigger_key"
    const val KEY_SELECTED_GPS_METHOD = "selected_gps_method"
    const val KEY_SCHEDULED_TIMES = "scheduled_times"
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_AMOLED_MODE = "amoled_mode"
    const val KEY_DYNAMIC_COLOR = "dynamic_color"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    const val KEY_DEVICE_ADMIN_ENABLED = "device_admin_enabled"
    const val KEY_LAST_LATITUDE = "last_latitude"
    const val KEY_LAST_LONGITUDE = "last_longitude"
    const val KEY_LAST_TIMESTAMP = "last_timestamp"
    const val KEY_GOOGLE_DRIVE_TOKEN = "google_drive_token"

    // Database
    const val DATABASE_NAME = "gps_location_tracker.db"
    const val LOCATION_TABLE = "location_data"

    // Services & Receivers
    const val ACTION_START_LOCATION_TRACKING = "com.gps.locationtracker.START_TRACKING"
    const val ACTION_STOP_LOCATION_TRACKING = "com.gps.locationtracker.STOP_TRACKING"
    const val ACTION_CAPTURE_LOCATION_SMS = "com.gps.locationtracker.CAPTURE_SMS"
    const val ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
    const val ACTION_SYNC_DRIVE = "com.gps.locationtracker.SYNC_DRIVE"

    // Notifications
    const val LOCATION_TRACKING_NOTIFICATION_ID = 1001
    const val LOCATION_TRACKING_CHANNEL_ID = "location_tracking_channel"
    const val LOCATION_TRACKING_CHANNEL_NAME = "Location Tracking"
    const val LOCATION_TRACKING_IMPORTANCE = 2 // NotificationManager.IMPORTANCE_LOW for silent

    // Location
    const val GPS_METHOD_FUSED = "FUSED_LOCATION"
    const val GPS_METHOD_NETWORK = "NETWORK_LOCATION"
    const val GPS_METHOD_GPS_ONLY = "GPS_ONLY"
    const val LOCATION_UPDATE_INTERVAL = 3600000L // 1 hour
    const val LOCATION_FASTEST_INTERVAL = 1800000L // 30 minutes
    const val LOCATION_PRIORITY = 100 // HIGH_ACCURACY

    // SMS Trigger
    const val SMS_TRIGGER_KEY_LENGTH = 64
    const val SMS_TRIGGER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    // Google Drive
    const val GOOGLE_DRIVE_FOLDER_NAME = "GPS Location Tracker"
    const val LOCATION_FILE_NAME = "location_data.txt"
    const val REQUEST_CODE_SIGN_IN = 9001

    // Developer Info
    const val DEVELOPER_EMAIL = "ap0803apap@gmail.com"
    const val DEVELOPER_GITHUB = "https://github.com/ap0803apap-sketch"
    const val DEVELOPER_NAME = "AP"

    // Device Admin
    const val DEVICE_ADMIN_COMPONENT = "com.gps.locationtracker.service.DeviceAdminReceiver"

    // Alarms & Scheduling
    const val ALARM_REQUEST_CODE_BASE = 1000
    const val SYNC_ALARM_REQUEST_CODE = 2000

    // File Paths
    const val OFFLINE_LOCATION_FILE = "location_data_offline.txt"
    const val BACKUP_LOCATION_FILE = "location_data_backup.txt"

    // Time Format
    const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val TIME_FORMAT = "HH:mm"

    // Retry Configuration
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 5000L // 5 seconds
    const val SYNC_INTERVAL_MINUTES = 30

    // Permission Request Codes
    const val PERMISSION_REQUEST_LOCATION = 100
    const val PERMISSION_REQUEST_SMS = 101
    const val PERMISSION_REQUEST_NOTIFICATION = 102
    const val PERMISSION_REQUEST_BIOMETRIC = 103

    // Biometric
    const val BIOMETRIC_TIMEOUT_SECONDS = 30

    // API Keys (will be configured at build time)
    const val GOOGLE_CLIENT_ID = "180849201429-ns6tgvkl6siiicjj5l71l4s17lkk1qns.apps.googleusercontent.com"
}

// GPS Methods available
enum class GPSMethod {
    FUSED_LOCATION_PROVIDER,
    NETWORK_LOCATION_PROVIDER,
    GPS_ONLY
}

// Theme options
enum class ThemeOption {
    LIGHT,
    DARK,
    SYSTEM
}

// Upload status
enum class UploadStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED,
    SYNCED
}
