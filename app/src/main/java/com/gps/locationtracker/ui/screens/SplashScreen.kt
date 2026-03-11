package com.gps.locationtracker.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.gps.locationtracker.R
import com.gps.locationtracker.viewmodel.AuthViewModel
import com.gps.locationtracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.Executors

@Composable
fun SplashScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isSetupComplete by authViewModel.isSetupComplete.collectAsState()
    val isAuthReady by authViewModel.isAuthReady.collectAsState()
    val isBiometricEnabled by settingsViewModel.isBiometricEnabled.collectAsState(initial = false)
    val context = LocalContext.current
    
    var showTryAgain by remember { mutableStateOf(false) }

    fun navigateAfterAuth() {
        if (isSetupComplete) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("setup") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    fun performAuth() {
        if (isLoggedIn) {
            if (isBiometricEnabled) {
                val activity = context.findActivity()
                if (activity != null) {
                    showTryAgain = false
                    showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            navigateAfterAuth()
                        },
                        onError = { error ->
                            Timber.e("Biometric error: $error")
                            showTryAgain = true
                        }
                    )
                } else {
                    navigateAfterAuth()
                }
            } else {
                navigateAfterAuth()
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    LaunchedEffect(isAuthReady) {
        if (!isAuthReady) return@LaunchedEffect
        // Ensure splash is visible for at least 1.5 seconds
        delay(1500)
        performAuth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "App Icon",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "GPS Location Tracker",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        if (showTryAgain) {
            Button(onClick = { performAuth() }) {
                Text("Try Again")
            }
        } else {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

private fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                activity.runOnUiThread { onSuccess() }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                activity.runOnUiThread { onError(errString.toString()) }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("App Locked")
        .setSubtitle("Authenticate using biometrics to continue")
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setNegativeButtonText("Exit App")
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        Timber.e("Biometric Authentication failed to start: ${e.message}")
        // If biometric not available/configured, proceed if it was somehow enabled in settings
        activity.runOnUiThread { onSuccess() }
    }
}
