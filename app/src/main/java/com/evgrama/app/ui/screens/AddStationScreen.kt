package com.evgrama.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.evgrama.app.data.ChargingHost
import com.evgrama.app.data.FirestoreService
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun AddStationScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestoreService = remember { FirestoreService() }
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var stationName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var socketType by remember { mutableStateOf("15A") }
    var pricePerHour by remember { mutableStateOf("") }
    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf("") }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (!granted) {
            Toast.makeText(context, "Location permission denied. Enter coordinates manually.", Toast.LENGTH_LONG).show()
        }
    }

    fun detectLocation() {
        isGettingLocation = true
        locationStatus = "Getting location..."
        coroutineScope.launch {
            try {
                val cts = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()

                if (location != null) {
                    latInput = String.format("%.6f", location.latitude)
                    lngInput = String.format("%.6f", location.longitude)
                    locationStatus = "✅ Location detected!"
                } else {
                    // Fallback to lastLocation
                    val last = fusedLocationClient.lastLocation.await()
                    if (last != null) {
                        latInput = String.format("%.6f", last.latitude)
                        lngInput = String.format("%.6f", last.longitude)
                        locationStatus = "✅ Location detected (cached)!"
                    } else {
                        locationStatus = "❌ Could not get GPS. Enter coordinates below manually."
                    }
                }
            } catch (e: Exception) {
                locationStatus = "❌ Error: ${e.message}. Enter coordinates manually."
            } finally {
                isGettingLocation = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Charging Station") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Station Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Tell users about your charging point.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = stationName,
                onValueChange = { stationName = it },
                label = { Text("Station / Shop Name *") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Raju Tea Stall") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Full Address *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GPS Detection Button
            OutlinedButton(
                onClick = {
                    if (hasLocationPermission) detectLocation()
                    else locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGettingLocation
            ) {
                if (isGettingLocation) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Getting Location...")
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detect My GPS Location")
                }
            }

            if (locationStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(locationStatus, style = MaterialTheme.typography.bodySmall, color = if (locationStatus.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual coordinate entry (always visible as fallback)
            Text("Location Coordinates *", style = MaterialTheme.typography.labelLarge)
            Text("(auto-filled by GPS, or enter manually)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latInput,
                    onValueChange = { latInput = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. 12.9716") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lngInput,
                    onValueChange = { lngInput = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. 77.5946") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Socket Type *", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("5A", "15A", "Type 2", "CCS").forEach { type ->
                    FilterChip(
                        selected = socketType == type,
                        onClick = { socketType = type },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pricePerHour,
                onValueChange = { pricePerHour = it },
                label = { Text("Price per Hour (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 20") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        // Validation
                        val lat = latInput.toDoubleOrNull()
                        val lng = lngInput.toDoubleOrNull()
                        val price = pricePerHour.toDoubleOrNull()

                        when {
                            stationName.isBlank() ->
                                Toast.makeText(context, "Enter station name", Toast.LENGTH_SHORT).show()
                            address.isBlank() ->
                                Toast.makeText(context, "Enter address", Toast.LENGTH_SHORT).show()
                            lat == null || lng == null ->
                                Toast.makeText(context, "Enter valid coordinates or use GPS button", Toast.LENGTH_SHORT).show()
                            price == null || price <= 0 ->
                                Toast.makeText(context, "Enter a valid price", Toast.LENGTH_SHORT).show()
                            else -> {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val uid = auth.currentUser?.uid
                                            ?: run {
                                                Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                                                isLoading = false
                                                return@launch
                                            }
                                        // Use uid + timestamp so a host can add multiple stations
                                        val stationId = "${uid}_${System.currentTimeMillis()}"

                                        val newHost = ChargingHost(
                                            id = stationId,
                                            name = stationName.trim(),
                                            address = address.trim(),
                                            latitude = lat,
                                            longitude = lng,
                                            socketType = socketType,
                                            pricePerHour = price,
                                            available = true,
                                            rating = 5.0,
                                            hostUid = uid
                                        )
                                        firestoreService.addChargingStation(newHost)
                                        Toast.makeText(context, "✅ Station added successfully!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "❌ Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp)
                ) {
                    Text("Register Station")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
