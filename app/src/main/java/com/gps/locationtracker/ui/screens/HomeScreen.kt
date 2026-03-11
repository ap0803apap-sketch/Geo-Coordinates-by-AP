package com.gps.locationtracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gps.locationtracker.viewmodel.AuthViewModel
import com.gps.locationtracker.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    locationViewModel: LocationViewModel,
    authViewModel: AuthViewModel
) {
    val lastLocation by locationViewModel.lastLocation.collectAsState()
    val liveLocation by locationViewModel.liveLocation.collectAsState()
    val locationCount by locationViewModel.locationCount.collectAsState()
    val isRefreshing by locationViewModel.isRefreshing.collectAsState()
    val isTrackingEnabled by locationViewModel.isTrackingEnabled.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Determine which location to display in the card (Live takes priority if available)
    val displayLocation = liveLocation ?: lastLocation

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GeoCoordinates") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Welcome to GeoCoordinates",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Tracking Toggle Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTrackingEnabled) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTrackingEnabled) "Tracking Active" else "Tracking Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isTrackingEnabled) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isTrackingEnabled) 
                                "Background service is running" 
                            else 
                                "Automatic logs are disabled",
                            fontSize = 12.sp,
                            color = if (isTrackingEnabled) 
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTrackingEnabled,
                        onCheckedChange = { locationViewModel.toggleTracking(it) }
                    )
                }
            }

            // Current Location Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text(
                                text = if (liveLocation != null) "Live Location" else "Last Captured Location",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        // Action Buttons (Refresh & Copy)
                        Row {
                            IconButton(
                                onClick = { 
                                    locationViewModel.refreshLocation() 
                                },
                                enabled = !isRefreshing
                            ) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (isRefreshing) 360f else 0f,
                                    animationSpec = tween(durationMillis = 1000),
                                    label = "rotation"
                                )
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = "Refresh Live Location",
                                    modifier = Modifier.rotate(rotation)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    displayLocation?.let { loc ->
                                        val textToCopy = """
                                            GeoCoordinates Location:
                                            Latitude: ${loc.latitude}
                                            Longitude: ${loc.longitude}
                                            Altitude: ${loc.altitude} m
                                            Accuracy: ${loc.accuracy} m
                                            Time: ${loc.formattedTime}
                                            Source: ${loc.source}
                                        """.trimIndent()
                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                        Toast.makeText(context, "Location copied to clipboard", Toast.LENGTH_SHORT).show()
                                    } ?: Toast.makeText(context, "No location data to copy", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Location Data")
                            }
                        }
                    }

                    if (displayLocation != null) {
                        LocationRow("Latitude", "%.6f".format(displayLocation.latitude))
                        LocationRow("Longitude", "%.6f".format(displayLocation.longitude))
                        LocationRow("Altitude", "%.2f m".format(displayLocation.altitude))
                        LocationRow("Accuracy", "%.1f m".format(displayLocation.accuracy))
                        LocationRow("Time", displayLocation.formattedTime)
                        
                        if (displayLocation.source == "LIVE") {
                            Text(
                                text = "Note: Live locations are not saved to logs",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Waiting for first GPS fix...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }

            // Stats Card (Clickable to go to Logs)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("logs") },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tracking History",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$locationCount logs saved",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        "VIEW ALL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "SMS Trigger is active. Send your key to this phone to get coordinates.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun LocationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
