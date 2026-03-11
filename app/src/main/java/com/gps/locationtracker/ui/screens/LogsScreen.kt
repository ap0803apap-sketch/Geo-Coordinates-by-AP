package com.gps.locationtracker.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gps.locationtracker.data.models.LocationData
import com.gps.locationtracker.utils.FileUtils
import com.gps.locationtracker.viewmodel.AuthViewModel
import com.gps.locationtracker.viewmodel.LocationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogsScreen(
    navController: NavHostController,
    locationViewModel: LocationViewModel,
    authViewModel: AuthViewModel
) {
    val allLocations by locationViewModel.allLocations.collectAsState()
    val isUploading by locationViewModel.isUploading.collectAsState()
    val isGuest by authViewModel.isGuest.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State for selection mode
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    
    // State for Dialogs
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var logToDeleteSingle by remember { mutableStateOf<LocationData?>(null) }

    // Handle back button when in selection mode
    BackHandler(isSelectionMode) {
        selectedIds = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedIds.size} Selected")
                    } else {
                        Text("Location Logs")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { showDeleteSelectedDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    } else if (allLocations.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All")
                        }
                    }
                },
                colors = if (isSelectionMode) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading Indicator for Upload
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Action Buttons
            if (allLocations.isNotEmpty() && !isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionIconButton(
                        icon = Icons.Default.Save,
                        label = "Save PDF",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val file = FileUtils.generateLogPdf(context, allLocations)
                            if (file != null) {
                                val success = FileUtils.saveFileToDownloads(context, file)
                                if (success) {
                                    Toast.makeText(context, "Saved to Downloads/${com.gps.locationtracker.utils.Constants.APP_NAME.replace(" ", "")}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    
                    ActionIconButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val file = FileUtils.generateLogPdf(context, allLocations)
                            if (file != null) {
                                FileUtils.shareFile(context, file)
                            }
                        }
                    )
                    
                    ActionIconButton(
                        icon = Icons.Default.CloudUpload,
                        label = "To Drive",
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading && !isGuest,
                        onClick = {
                            val file = FileUtils.generateLogPdf(context, allLocations)
                            if (file != null) {
                                locationViewModel.uploadToDrive(file) { result ->
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Uploaded successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Upload failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (allLocations.isEmpty()) {
                EmptyLogsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allLocations, key = { it.id }) { location ->
                        val isSelected = selectedIds.contains(location.id)
                        
                        LogItem(
                            location = location,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedIds = setOf(location.id)
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) {
                                        selectedIds - location.id
                                    } else {
                                        selectedIds + location.id
                                    }
                                }
                            },
                            onDeleteClick = {
                                logToDeleteSingle = location
                            },
                            onToggleSelection = {
                                selectedIds = if (isSelected) {
                                    selectedIds - location.id
                                } else {
                                    selectedIds + location.id
                                }
                            }
                        )
                    }
                }
            }
        }

        // Delete All Confirmation
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Clear All Logs?") },
                text = { Text("This will permanently delete all ${allLocations.size} location records.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            locationViewModel.clearAllLogs()
                            showDeleteAllDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("CLEAR ALL")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        // Delete Selected Confirmation
        if (showDeleteSelectedDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteSelectedDialog = false },
                title = { Text("Delete Selected?") },
                text = { Text("Are you sure you want to delete the ${selectedIds.size} selected logs?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            locationViewModel.deleteMultipleLocations(selectedIds.toList())
                            selectedIds = emptySet()
                            showDeleteSelectedDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("DELETE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSelectedDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        // Delete Single Confirmation
        logToDeleteSingle?.let { location ->
            AlertDialog(
                onDismissRequest = { logToDeleteSingle = null },
                title = { Text("Delete Log?") },
                text = { Text("Delete location captured at ${location.formattedTime}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            locationViewModel.deleteLocation(location.id)
                            logToDeleteSingle = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("DELETE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { logToDeleteSingle = null }) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}

@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                icon, 
                contentDescription = label, 
                modifier = Modifier.size(20.dp),
                tint = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                label, 
                fontSize = 10.sp,
                color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItem(
    location: LocationData,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = location.formattedTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = location.source,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lat: ${"%.6f".format(location.latitude)}", fontSize = 13.sp)
                        Text("Lon: ${"%.6f".format(location.longitude)}", fontSize = 13.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Acc: ${"%.1f".format(location.accuracy)}m", fontSize = 13.sp)
                        Text("Alt: ${"%.1f".format(location.altitude)}m", fontSize = 13.sp)
                    }
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLogsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No logs found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
