package com.gps.locationtracker.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.gps.locationtracker.data.repository.LocationRepository
import com.gps.locationtracker.data.repository.PreferencesRepository
import com.gps.locationtracker.data.repository.GoogleDriveRepository
import com.gps.locationtracker.data.models.LocationData
import com.gps.locationtracker.service.DeviceAdminReceiver
import com.gps.locationtracker.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

// Auth ViewModel
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application.applicationContext)
    private val locationRepository = LocationRepository(application.applicationContext)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _isSetupComplete = MutableStateFlow(false)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    private val _isAuthReady = MutableStateFlow(false)
    val isAuthReady: StateFlow<Boolean> = _isAuthReady.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _smsTriggerKey = MutableStateFlow("")
    val smsTriggerKey: StateFlow<String> = _smsTriggerKey.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            preferencesRepository.getUserInfo().collect { userPrefs ->
                _isLoggedIn.value = userPrefs.isLoggedIn
                _isGuest.value = userPrefs.isGuest
                _isSetupComplete.value = userPrefs.isSetupComplete
                _userEmail.value = userPrefs.email
                _userName.value = userPrefs.name
                _smsTriggerKey.value = userPrefs.smsTriggerKey
                _isAuthReady.value = true
                Timber.d("User info loaded: isLoggedIn=${userPrefs.isLoggedIn}, isGuest=${userPrefs.isGuest}, isSetupComplete=${userPrefs.isSetupComplete}")
            }
        }
    }

    fun loginWithGoogle(userId: String, email: String, name: String, profileUrl: String?) {
        viewModelScope.launch {
            preferencesRepository.saveUserInfo(userId, email, name, profileUrl)
            _isLoggedIn.value = true
            _isGuest.value = false
            _userEmail.value = email
            _userName.value = name
            Timber.d("User logged in with Google: $email")
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            preferencesRepository.saveGuestLogin()
            _isLoggedIn.value = true
            _isGuest.value = true
            _userEmail.value = "Guest"
            _userName.value = "Guest User"
            Timber.d("User logged in as Guest")
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesRepository.clearUserInfo()
            preferencesRepository.clearGoogleDriveToken()
            _isLoggedIn.value = false
            _isGuest.value = false
            _isSetupComplete.value = false
            _userEmail.value = ""
            _userName.value = ""
            Timber.d("User logged out")
        }
    }

    fun generateAndSaveSMSTriggerKey(): String {
        val key = generateSMSTriggerKey()
        viewModelScope.launch {
            preferencesRepository.saveSMSTriggerKey(key)
            _smsTriggerKey.value = key
            _isSetupComplete.value = true
            Timber.d("SMS trigger key generated and saved")
        }
        return key
    }

    fun regenerateSMSTriggerKey(): String {
        val key = generateSMSTriggerKey()
        viewModelScope.launch {
            preferencesRepository.saveSMSTriggerKey(key)
            _smsTriggerKey.value = key
            Timber.d("SMS trigger key regenerated")
        }
        return key
    }

    private fun generateSMSTriggerKey(): String {
        val length = Constants.SMS_TRIGGER_KEY_LENGTH
        val chars = Constants.SMS_TRIGGER_CHARS
        val random = Random()
        val key = StringBuilder()

        repeat(length) {
            key.append(chars[random.nextInt(chars.length)])
        }

        return key.toString()
    }
}

// Location ViewModel
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application.applicationContext)
    private val preferencesRepository = PreferencesRepository(application.applicationContext)
    private val driveRepository = GoogleDriveRepository(application.applicationContext)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val sdf = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.US)

    private val _allLocations = MutableStateFlow<List<LocationData>>(emptyList())
    val allLocations: StateFlow<List<LocationData>> = _allLocations.asStateFlow()

    private val _lastLocation = MutableStateFlow<LocationData?>(null)
    val lastLocation: StateFlow<LocationData?> = _lastLocation.asStateFlow()

    private val _liveLocation = MutableStateFlow<LocationData?>(null)
    val liveLocation: StateFlow<LocationData?> = _liveLocation.asStateFlow()

    private val _locationCount = MutableStateFlow(0)
    val locationCount: StateFlow<Int> = _locationCount.asStateFlow()

    private val _syncedCount = MutableStateFlow(0)
    val syncedCount: StateFlow<Int> = _syncedCount.asStateFlow()
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadAllLocations()
        loadLocationStats()
        setupLogCleanup()
    }

    private fun setupLogCleanup() {
        viewModelScope.launch {
            preferencesRepository.getLogRetentionDays().collectLatest { days ->
                if (days > 0) {
                    locationRepository.deleteLocationsOlderThan(days)
                    loadLocationStats()
                }
            }
        }
    }

    private fun loadAllLocations() {
        viewModelScope.launch {
            locationRepository.getAllLocations().collect { locations ->
                _allLocations.value = locations
                if (locations.isNotEmpty()) {
                    _lastLocation.value = locations.first()
                } else {
                    _lastLocation.value = null
                }
            }
        }
    }

    private fun loadLocationStats() {
        viewModelScope.launch {
            val total = locationRepository.getLocationCount()
            val synced = locationRepository.getSyncedCount()
            _locationCount.value = total
            _syncedCount.value = synced
            Timber.d("Locations: total=$total, synced=$synced")
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // We don't check permissions here as it's expected to be granted at this stage
                // but in a real app, a check would be safer.
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val liveData = LocationData(
                                id = -1, // Temporary ID
                                latitude = location.latitude,
                                longitude = location.longitude,
                                altitude = location.altitude,
                                accuracy = location.accuracy,
                                timestamp = System.currentTimeMillis(),
                                formattedTime = sdf.format(Date()),
                                source = "LIVE"
                            )
                            _liveLocation.value = liveData
                            Timber.d("Live location refreshed: ${location.latitude}, ${location.longitude}")
                        }
                        _isRefreshing.value = false
                    }
                    .addOnFailureListener {
                        Timber.e("Failed to refresh live location: ${it.message}")
                        _isRefreshing.value = false
                    }
            } catch (e: SecurityException) {
                Timber.e("Security exception refreshing location: ${e.message}")
                _isRefreshing.value = false
            } catch (e: Exception) {
                Timber.e("Unexpected error refreshing location: ${e.message}")
                _isRefreshing.value = false
            }
        }
    }

    fun saveLocation(latitude: Double, longitude: Double, source: String = "MANUAL") {
        viewModelScope.launch {
            val id = locationRepository.insertLocation(latitude, longitude, source = source)
            if (id > 0) {
                preferencesRepository.saveLastLocation(latitude, longitude)
                Timber.d("Location saved: $latitude, $longitude")
                loadLocationStats()
            }
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            locationRepository.deleteLocation(id)
            loadLocationStats()
        }
    }

    fun deleteMultipleLocations(ids: List<Int>) {
        viewModelScope.launch {
            locationRepository.deleteMultipleLocations(ids)
            loadLocationStats()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            locationRepository.deleteLocationsOlderThan(0)
            loadLocationStats()
        }
    }

    fun uploadToDrive(file: File, onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = driveRepository.uploadPdfToDrive(file)
            _isUploading.value = false
            onComplete(result)
        }
    }

    fun updateLocationSyncStatus(id: Int, synced: Boolean) {
        viewModelScope.launch {
            locationRepository.updateLocationStatus(id, "UPLOADED", synced)
            Timber.d("Location $id sync status updated: $synced")
            loadLocationStats()
        }
    }

    fun deleteLocationsOlderThan(days: Int) {
        viewModelScope.launch {
            locationRepository.deleteLocationsOlderThan(days)
            loadAllLocations()
            loadLocationStats()
            Timber.d("Deleted locations older than $days days")
        }
    }

    fun exportLocationData(): String {
        var csv = ""
        viewModelScope.launch {
            csv = locationRepository.exportLocationsAsCsv()
        }
        return csv
    }
}

// Settings ViewModel
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application.applicationContext)
    private val context = application.applicationContext

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAmoledMode = MutableStateFlow(false)
    val isAmoledMode: StateFlow<Boolean> = _isAmoledMode.asStateFlow()

    private val _isDynamicColor = MutableStateFlow(true)
    val isDynamicColor: StateFlow<Boolean> = _isDynamicColor.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isDeviceAdminEnabled = MutableStateFlow(false)
    val isDeviceAdminEnabled: StateFlow<Boolean> = _isDeviceAdminEnabled.asStateFlow()

    private val _selectedGpsMethod = MutableStateFlow(Constants.GPS_METHOD_FUSED)
    val selectedGpsMethod: StateFlow<String> = _selectedGpsMethod.asStateFlow()
    
    private val _trackingInterval = MutableStateFlow(Constants.LOCATION_UPDATE_INTERVAL)
    val trackingInterval: StateFlow<Long> = _trackingInterval.asStateFlow()

    private val _logRetentionDays = MutableStateFlow(30)
    val logRetentionDays: StateFlow<Int> = _logRetentionDays.asStateFlow()

    init {
        loadSettings()
        syncActualDeviceAdminState()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.isDarkMode().collect { _isDarkMode.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.isAmoledMode().collect { _isAmoledMode.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.isDynamicColor().collect { _isDynamicColor.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.isBiometricEnabled().collect { _isBiometricEnabled.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.isDeviceAdminEnabled().collect { _isDeviceAdminEnabled.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.getSelectedGpsMethod().collect { _selectedGpsMethod.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.getTrackingInterval().collect { _trackingInterval.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.getLogRetentionDays().collect { _logRetentionDays.value = it }
        }
    }

    fun syncActualDeviceAdminState() {
        val isActive = DeviceAdminReceiver.isAdminActive(context)
        if (isActive != _isDeviceAdminEnabled.value) {
            setDeviceAdminEnabled(isActive)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
            _isDarkMode.value = enabled
            Timber.d("Dark mode set to: $enabled")
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAmoledMode(enabled)
            _isAmoledMode.value = enabled
            Timber.d("AMOLED mode set to: $enabled")
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColor(enabled)
            _isDynamicColor.value = enabled
            Timber.d("Dynamic color set to: $enabled")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
            _isBiometricEnabled.value = enabled
            Timber.d("Biometric enabled set to: $enabled")
        }
    }

    fun setDeviceAdminEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDeviceAdminEnabled(enabled)
            _isDeviceAdminEnabled.value = enabled
            Timber.d("Device admin enabled set to: $enabled")
        }
    }

    fun setSelectedGpsMethod(method: String) {
        viewModelScope.launch {
            preferencesRepository.saveSelectedGpsMethod(method)
            _selectedGpsMethod.value = method
            Timber.d("GPS method set to: $method")
        }
    }
    
    fun setTrackingInterval(intervalMs: Long) {
        viewModelScope.launch {
            preferencesRepository.setTrackingInterval(intervalMs)
            _trackingInterval.value = intervalMs
            Timber.d("Tracking interval set to: $intervalMs ms")
        }
    }

    fun setLogRetentionDays(days: Int) {
        viewModelScope.launch {
            preferencesRepository.setLogRetentionDays(days)
            _logRetentionDays.value = days
            Timber.d("Log retention set to: $days days")
        }
    }
}
