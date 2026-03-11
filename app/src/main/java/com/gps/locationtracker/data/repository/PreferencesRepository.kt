package com.gps.locationtracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.gps.locationtracker.data.models.UserPreferences
import com.gps.locationtracker.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

class PreferencesRepository(private val context: Context) {

    private val dataStore: DataStore<Preferences> = context.preferencesDataStore

    // Create all preference keys
    private val userIdKey = stringPreferencesKey(Constants.KEY_USER_ID)
    private val userEmailKey = stringPreferencesKey(Constants.KEY_USER_EMAIL)
    private val userNameKey = stringPreferencesKey(Constants.KEY_USER_NAME)
    private val userProfilePictureKey = stringPreferencesKey(Constants.KEY_USER_PROFILE_PICTURE)
    private val isLoggedInKey = booleanPreferencesKey(Constants.KEY_IS_LOGGED_IN)
    private val isSetupCompleteKey = booleanPreferencesKey(Constants.KEY_IS_SETUP_COMPLETE)
    private val smsTriggerKeyKey = stringPreferencesKey(Constants.KEY_SMS_TRIGGER_KEY)
    private val selectedGpsMethodKey = stringPreferencesKey(Constants.KEY_SELECTED_GPS_METHOD)
    private val darkModeKey = booleanPreferencesKey(Constants.KEY_DARK_MODE)
    private val amoledModeKey = booleanPreferencesKey(Constants.KEY_AMOLED_MODE)
    private val dynamicColorKey = booleanPreferencesKey(Constants.KEY_DYNAMIC_COLOR)
    private val biometricEnabledKey = booleanPreferencesKey(Constants.KEY_BIOMETRIC_ENABLED)
    private val deviceAdminEnabledKey = booleanPreferencesKey(Constants.KEY_DEVICE_ADMIN_ENABLED)
    private val lastLatitudeKey = doublePreferencesKey(Constants.KEY_LAST_LATITUDE)
    private val lastLongitudeKey = doublePreferencesKey(Constants.KEY_LAST_LONGITUDE)
    private val lastTimestampKey = longPreferencesKey(Constants.KEY_LAST_TIMESTAMP)
    private val googleDriveTokenKey = stringPreferencesKey(Constants.KEY_GOOGLE_DRIVE_TOKEN)
    
    // New Keys for tracking configuration
    private val trackingIntervalKey = longPreferencesKey("tracking_interval")
    private val logRetentionDaysKey = intPreferencesKey("log_retention_days")

    // User Authentication
    suspend fun saveUserInfo(userId: String, email: String, name: String, profilePictureUrl: String? = null) {
        try {
            dataStore.edit { preferences ->
                preferences[userIdKey] = userId
                preferences[userEmailKey] = email
                preferences[userNameKey] = name
                profilePictureUrl?.let { preferences[userProfilePictureKey] = it }
                preferences[isLoggedInKey] = true
            }
            Timber.d("Saved user info: $email")
        } catch (e: Exception) {
            Timber.e("Error saving user info: ${e.message}")
        }
    }

    suspend fun clearUserInfo() {
        try {
            dataStore.edit { preferences ->
                preferences.remove(userIdKey)
                preferences.remove(userEmailKey)
                preferences.remove(userNameKey)
                preferences.remove(userProfilePictureKey)
                preferences.remove(isLoggedInKey)
                preferences.remove(googleDriveTokenKey)
            }
            Timber.d("Cleared user info")
        } catch (e: Exception) {
            Timber.e("Error clearing user info: ${e.message}")
        }
    }

    fun getUserInfo(): Flow<UserPreferences> {
        return dataStore.data.map { preferences ->
            UserPreferences(
                userId = preferences[userIdKey] ?: "",
                email = preferences[userEmailKey] ?: "",
                name = preferences[userNameKey] ?: "",
                profilePictureUrl = preferences[userProfilePictureKey],
                isLoggedIn = preferences[isLoggedInKey] ?: false,
                isSetupComplete = preferences[isSetupCompleteKey] ?: false,
                smsTriggerKey = preferences[smsTriggerKeyKey] ?: "",
                selectedGpsMethod = preferences[selectedGpsMethodKey] ?: Constants.GPS_METHOD_FUSED,
                isDarkMode = preferences[darkModeKey] ?: false,
                isAmoledMode = preferences[amoledModeKey] ?: false,
                isDynamicColor = preferences[dynamicColorKey] ?: true,
                isBiometricEnabled = preferences[biometricEnabledKey] ?: false,
                isDeviceAdminEnabled = preferences[deviceAdminEnabledKey] ?: false,
                lastLatitude = preferences[lastLatitudeKey] ?: 0.0,
                lastLongitude = preferences[lastLongitudeKey] ?: 0.0,
                lastTimestamp = preferences[lastTimestampKey] ?: 0L,
                googleDriveToken = preferences[googleDriveTokenKey]
            )
        }
    }

    fun getIsLoggedIn(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[isLoggedInKey] ?: false
        }
    }

    // Tracking Configuration
    suspend fun setTrackingInterval(intervalMs: Long) {
        dataStore.edit { it[trackingIntervalKey] = intervalMs }
    }

    fun getTrackingInterval(): Flow<Long> {
        return dataStore.data.map { it[trackingIntervalKey] ?: Constants.LOCATION_UPDATE_INTERVAL }
    }

    suspend fun setLogRetentionDays(days: Int) {
        dataStore.edit { it[logRetentionDaysKey] = days }
    }

    fun getLogRetentionDays(): Flow<Int> {
        return dataStore.data.map { it[logRetentionDaysKey] ?: 30 } // Default 30 days
    }

    // SMS Trigger Setup
    suspend fun saveSMSTriggerKey(key: String) {
        try {
            dataStore.edit { preferences ->
                preferences[smsTriggerKeyKey] = key
                preferences[isSetupCompleteKey] = true
            }
            Timber.d("Saved SMS trigger key")
        } catch (e: Exception) {
            Timber.e("Error saving SMS trigger key: ${e.message}")
        }
    }

    fun getSMSTriggerKey(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[smsTriggerKeyKey] ?: ""
        }
    }

    // GPS Method Settings
    suspend fun saveSelectedGpsMethod(method: String) {
        try {
            dataStore.edit { preferences ->
                preferences[selectedGpsMethodKey] = method
            }
            Timber.d("Saved GPS method: $method")
        } catch (e: Exception) {
            Timber.e("Error saving GPS method: ${e.message}")
        }
    }

    fun getSelectedGpsMethod(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[selectedGpsMethodKey] ?: Constants.GPS_METHOD_FUSED
        }
    }

    // Theme Settings
    suspend fun setDarkMode(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[darkModeKey] = enabled
            }
            Timber.d("Set dark mode: $enabled")
        } catch (e: Exception) {
            Timber.e("Error setting dark mode: ${e.message}")
        }
    }

    fun isDarkMode(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[darkModeKey] ?: false
        }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[amoledModeKey] = enabled
            }
            Timber.d("Set AMOLED mode: $enabled")
        } catch (e: Exception) {
            Timber.e("Error setting AMOLED mode: ${e.message}")
        }
    }

    fun isAmoledMode(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[amoledModeKey] ?: false
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[dynamicColorKey] = enabled
            }
            Timber.d("Set dynamic color: $enabled")
        } catch (e: Exception) {
            Timber.e("Error setting dynamic color: ${e.message}")
        }
    }

    fun isDynamicColor(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[dynamicColorKey] ?: true
        }
    }

    // Biometric Authentication
    suspend fun setBiometricEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[biometricEnabledKey] = enabled
            }
            Timber.d("Set biometric enabled: $enabled")
        } catch (e: Exception) {
            Timber.e("Error setting biometric: ${e.message}")
        }
    }

    fun isBiometricEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[biometricEnabledKey] ?: false
        }
    }

    // Device Admin
    suspend fun setDeviceAdminEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[deviceAdminEnabledKey] = enabled
            }
            Timber.d("Set device admin enabled: $enabled")
        } catch (e: Exception) {
            Timber.e("Error setting device admin: ${e.message}")
        }
    }

    fun isDeviceAdminEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[deviceAdminEnabledKey] ?: false
        }
    }

    // Location Cache
    suspend fun saveLastLocation(latitude: Double, longitude: Double) {
        try {
            dataStore.edit { preferences ->
                preferences[lastLatitudeKey] = latitude
                preferences[lastLongitudeKey] = longitude
                preferences[lastTimestampKey] = System.currentTimeMillis()
            }
            Timber.d("Saved last location: $latitude, $longitude")
        } catch (e: Exception) {
            Timber.e("Error saving last location: ${e.message}")
        }
    }

    fun getLastLocation(): Flow<Pair<Double, Double>> {
        return dataStore.data.map { preferences ->
            Pair(
                preferences[lastLatitudeKey] ?: 0.0,
                preferences[lastLongitudeKey] ?: 0.0
            )
        }
    }

    // Google Drive Token
    suspend fun saveGoogleDriveToken(token: String) {
        try {
            dataStore.edit { preferences ->
                preferences[googleDriveTokenKey] = token
            }
            Timber.d("Saved Google Drive token")
        } catch (e: Exception) {
            Timber.e("Error saving Google Drive token: ${e.message}")
        }
    }

    fun getGoogleDriveToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[googleDriveTokenKey]
        }
    }

    suspend fun clearGoogleDriveToken() {
        try {
            dataStore.edit { preferences ->
                preferences.remove(googleDriveTokenKey)
            }
            Timber.d("Cleared Google Drive token")
        } catch (e: Exception) {
            Timber.e("Error clearing Google Drive token: ${e.message}")
        }
    }
}
