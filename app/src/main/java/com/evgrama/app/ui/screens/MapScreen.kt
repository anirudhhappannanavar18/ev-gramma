package com.evgrama.app.ui.screens

import android.Manifest
import android.widget.Toast
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.evgrama.app.data.ChargingHost
import com.evgrama.app.data.FirestoreService
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBookClick: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestoreService = remember { FirestoreService() }
    val coroutineScope = rememberCoroutineScope()

    var chargingHosts by remember { mutableStateOf<List<ChargingHost>>(emptyList()) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    LaunchedEffect(Unit) {
        // Initialize osmdroid config
        Configuration.getInstance().userAgentValue = context.packageName

        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Load chargers from Firestore
        try {
            chargingHosts = firestoreService.getChargingHosts()
        } catch (e: Exception) {
            Toast.makeText(context, "Map Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        isInitialLoading = false
    }

    var selectedHost by remember { mutableStateOf<ChargingHost?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Chargers") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        controller.setCenter(GeoPoint(12.9716, 77.5946))
                    }
                },
                update = { mapView ->
                    mapViewRef = mapView
                    mapView.overlays.removeAll { it is Marker }
                    chargingHosts.forEach { host ->
                        val marker = Marker(mapView)
                        marker.position = GeoPoint(host.latitude, host.longitude)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = host.name
                        marker.setOnMarkerClickListener { _, _ ->
                            selectedHost = host
                            true
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Booking Selection Dialog
            selectedHost?.let { host ->
                AlertDialog(
                    onDismissRequest = { selectedHost = null },
                    title = { Text(host.name) },
                    text = { 
                        Column {
                            Text(host.address)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Rate: ₹${host.pricePerHour}/hr", fontWeight = FontWeight.Bold)
                            Text("Socket: ${host.socketType}")
                        }
                    },
                    confirmButton = {
                        Button(onClick = { 
                            selectedHost = null
                            onBookClick(host.id) 
                        }) {
                            Text("Book Now")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedHost = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Go to My Location FAB
            FloatingActionButton(
                onClick = {
                    if (hasLocationPermission) {
                        coroutineScope.launch {
                            try {
                                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                val location = fusedClient.lastLocation.await()
                                if (location != null) {
                                    mapViewRef?.controller?.animateTo(
                                        GeoPoint(location.latitude, location.longitude)
                                    )
                                    mapViewRef?.controller?.setZoom(15.0)
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("📍", style = MaterialTheme.typography.titleLarge)
            }

            // Loading / No data indicator
            if (isInitialLoading) {
                Card(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                ) {
                    Text("Searching for chargers...", modifier = Modifier.padding(16.dp))
                }
            } else if (chargingHosts.isEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                ) {
                    Text("No charging stations found. Be the first to add one as a Host!", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
