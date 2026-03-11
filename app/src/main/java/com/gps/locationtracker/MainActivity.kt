package com.gps.locationtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gps.locationtracker.service.LocationTrackingService
import com.gps.locationtracker.ui.screens.*
import com.gps.locationtracker.ui.theme.GPSLocationTrackerTheme
import com.gps.locationtracker.viewmodel.AuthViewModel
import com.gps.locationtracker.viewmodel.LocationViewModel
import com.gps.locationtracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

// Changed to AppCompatActivity to support BiometricPrompt
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            Timber.d("Location permissions granted via launcher")
            lifecycleScope.launch {
                val isEnabled = locationViewModel.isTrackingEnabled.value
                if (isEnabled) {
                    startLocationService()
                }
            }
        } else {
            Timber.e("Location permissions denied via launcher")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG && Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState(initial = false)
            val isAmoledMode by settingsViewModel.isAmoledMode.collectAsState(initial = false)
            val isDynamicColor by settingsViewModel.isDynamicColor.collectAsState(initial = false)

            GPSLocationTrackerTheme(
                darkTheme = isDarkMode,
                dynamicColor = isDynamicColor,
                amoledMode = isAmoledMode
            ) {
                MainNavigation(
                    authViewModel = authViewModel,
                    locationViewModel = locationViewModel,
                    settingsViewModel = settingsViewModel,
                    onRequestPermissions = { requestPermissions() }
                )
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            lifecycleScope.launch {
                val isEnabled = locationViewModel.isTrackingEnabled.value
                if (isEnabled) {
                    startLocationService()
                }
            }
        }
    }

    private fun startLocationService() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (hasFine || hasCoarse) {
            val serviceIntent = Intent(this, LocationTrackingService::class.java)
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
                Timber.d("Location service started successfully")
            } catch (e: Exception) {
                Timber.e("Failed to start service: ${e.message}")
            }
        } else {
            Timber.w("Cannot start location service: Permissions NOT granted")
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if tracking is enabled before starting service
        lifecycleScope.launch {
            // Wait for first value if necessary, but value is usually available
            val isEnabled = locationViewModel.isTrackingEnabled.value
            if (isEnabled) {
                startLocationService()
            } else {
                Timber.d("Tracking disabled, not starting service in onResume")
                // Ensure service is stopped if it's running
                LocationTrackingService.stopLocationTracking(this@MainActivity)
            }
        }
    }
}

@Composable
fun MainNavigation(
    authViewModel: AuthViewModel,
    locationViewModel: LocationViewModel,
    settingsViewModel: SettingsViewModel,
    onRequestPermissions: () -> Unit
) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isSetupComplete by authViewModel.isSetupComplete.collectAsState()
    val isAuthReady by authViewModel.isAuthReady.collectAsState()

    LaunchedEffect(isAuthReady, isLoggedIn, isSetupComplete) {
        if (!isAuthReady) return@LaunchedEffect

        val currentRoute = navController.currentDestination?.route
        
        // Don't auto-navigate if we are on splash, let splash handle it
        if (currentRoute == "splash") return@LaunchedEffect

        if (isLoggedIn) {
            if (isSetupComplete) {
                if (currentRoute != "home" && currentRoute != "settings" && currentRoute != "logs") {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                if (currentRoute != "setup") {
                    navController.navigate("setup") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        } else {
            if (currentRoute != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController, authViewModel, settingsViewModel)
        }
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        composable("setup") {
            SetupScreen(navController, authViewModel, locationViewModel, settingsViewModel)
        }
        composable("home") {
            HomeScreen(navController, locationViewModel, authViewModel)
        }
        composable("settings") {
            SettingsScreen(navController, authViewModel, settingsViewModel)
        }
        composable("logs") {
            LogsScreen(navController, locationViewModel, authViewModel)
        }
    }
}
