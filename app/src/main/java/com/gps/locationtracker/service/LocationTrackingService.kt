package com.gps.locationtracker.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.gps.locationtracker.MainActivity
import com.gps.locationtracker.R
import com.gps.locationtracker.data.repository.LocationRepository
import com.gps.locationtracker.data.repository.PreferencesRepository
import com.gps.locationtracker.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRepository: LocationRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var locationManager: LocationManager
    private lateinit var locationCallback: LocationCallback
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var preferenceJob: Job? = null
    private val sdf = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.US)
    private var isTracking = false
    private var currentInterval = Constants.LOCATION_UPDATE_INTERVAL

    override fun onCreate() {
        super.onCreate()
        Timber.d("LocationTrackingService created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRepository = LocationRepository(this)
        preferencesRepository = PreferencesRepository(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        createNotificationChannel()
        setupLocationCallback()
        
        // Observe interval changes
        preferenceJob = coroutineScope.launch {
            preferencesRepository.getTrackingInterval()
                .distinctUntilChanged()
                .collect { newInterval ->
                    Timber.d("Tracking interval changed: $newInterval")
                    if (isTracking && newInterval != currentInterval) {
                        currentInterval = newInterval
                        restartTracking()
                    } else {
                        currentInterval = newInterval
                    }
                }
        }
    }

    private fun restartTracking() {
        Timber.d("Restarting tracking with new interval: $currentInterval")
        stopLocationTracking()
        startLocationTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("LocationTrackingService onStartCommand with action: ${intent?.action}")

        // Handle Stop action immediately to stop service
        if (intent?.action == Constants.ACTION_STOP_LOCATION_TRACKING) {
            stopLocationTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val hasFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (!hasFine && !hasCoarse) {
            Timber.e("Service started without location permissions. Stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.LOCATION_TRACKING_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(Constants.LOCATION_TRACKING_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Timber.e("Failed to start foreground service: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            Constants.ACTION_START_LOCATION_TRACKING -> startLocationTracking()
            Constants.ACTION_CAPTURE_LOCATION_SMS -> captureSingleLocation("SMS")
            else -> {
                // Check preference before starting tracking for default actions or process restart
                coroutineScope.launch {
                    val isEnabled = preferencesRepository.isTrackingEnabled().first()
                    if (isEnabled) {
                        if (!isTracking) startLocationTracking()
                    } else {
                        Timber.d("Tracking is disabled in preferences. Stopping service.")
                        stopLocationTracking()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun captureSingleLocation(source: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    Timber.d("Single location captured from $source: ${it.latitude}, ${it.longitude}")
                    saveLocationData(it, source)
                }
            }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    saveLocationData(location, "AUTO")
                }
            }
        }
    }

    private fun saveLocationData(location: Location, source: String) {
        coroutineScope.launch {
            try {
                val id = locationRepository.insertLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    source = source
                )

                if (id > 0) {
                    preferencesRepository.saveLastLocation(location.latitude, location.longitude)
                    locationRepository.saveToOfflineFile(
                        com.gps.locationtracker.data.models.LocationData(
                            id = id.toInt(),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            accuracy = location.accuracy,
                            timestamp = System.currentTimeMillis(),
                            formattedTime = sdf.format(Date()),
                            source = source
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e("Error saving location: ${e.message}")
            }
        }
    }

    private fun startLocationTracking() {
        if (isTracking) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, currentInterval)
            .setMinUpdateIntervalMillis(currentInterval / 2)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isTracking = true
            Timber.d("Location tracking started with interval: $currentInterval")
        } catch (e: Exception) {
            Timber.e("Error starting location tracking: ${e.message}")
        }
    }

    private fun stopLocationTracking() {
        if (!isTracking) return
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
            Timber.d("Location tracking stopped")
        } catch (e: Exception) {
            Timber.e("Error stopping location tracking: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.LOCATION_TRACKING_CHANNEL_ID,
                Constants.LOCATION_TRACKING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, Constants.LOCATION_TRACKING_CHANNEL_ID)
            .setContentTitle("GPS Location Tracker")
            .setContentText("Tracking frequency: ${getIntervalLabel(currentInterval)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getIntervalLabel(ms: Long): String {
        return when (ms) {
            30000L -> "30 Sec"
            60000L -> "1 Min"
            300000L -> "5 Min"
            900000L -> "15 Min"
            1800000L -> "30 Min"
            3600000L -> "1 Hour"
            else -> "${ms/1000}s"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceJob?.cancel()
        stopLocationTracking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startLocationTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = Constants.ACTION_START_LOCATION_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopLocationTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = Constants.ACTION_STOP_LOCATION_TRACKING
            }
            // Use startService to send the STOP action intent
            context.startService(intent)
        }
    }
}
