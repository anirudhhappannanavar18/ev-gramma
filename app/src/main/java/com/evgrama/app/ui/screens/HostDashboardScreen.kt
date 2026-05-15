package com.evgrama.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.evgrama.app.R
import androidx.compose.ui.unit.dp
import com.evgrama.app.data.BookingRecord
import com.evgrama.app.data.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostDashboardScreen(
    onNavigateToAddStation: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestoreService = remember { FirestoreService() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isAvailable by remember { mutableStateOf(true) }
    var hostName by remember { mutableStateOf("") }
    var todayEarnings by remember { mutableStateOf(0.0) }
    var pendingBookings by remember { mutableStateOf<List<BookingRecord>>(emptyList()) }
    var myStations by remember { mutableStateOf<List<com.evgrama.app.data.ChargingHost>>(emptyList()) }
    var isRefreshingStations by remember { mutableStateOf(false) }

    fun refreshMyStations() {
        coroutineScope.launch {
            isRefreshingStations = true
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val allHosts = firestoreService.getAllChargingStations()
                myStations = allHosts.filter { it.hostUid == uid }
                
                val allBookings = firestoreService.getBookingHistory(uid, "host")
                pendingBookings = allBookings.filter { it.status == "Pending" }
                todayEarnings = allBookings.filter { it.status == "Completed" }.sumOf { it.amountPaid }
                
                // Set isAvailable based on the first station for simplicity
                if (myStations.isNotEmpty()) {
                    isAvailable = myStations.first().available
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                isRefreshingStations = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val profile = firestoreService.getUserProfile(uid)
        hostName = profile?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "Host"
        refreshMyStations()
    }

    val statusColor = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusText = if (isAvailable) stringResource(R.string.online_msg) else stringResource(R.string.offline_msg)

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddStation,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Station") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.host_dashboard), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (hostName.isNotBlank()) {
                        Text("Hello, $hostName", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.List, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                ) {
                    Column {
                        Text("Active Host Status", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Manage your stations individually below", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.today_earnings), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(
                            "₹${String.format("%.0f", todayEarnings)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Req", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(
                            "${pendingBookings.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your Stations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { refreshMyStations() }) {
                    Text(if (isRefreshingStations) "Refreshing..." else "Refresh")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (myStations.isEmpty()) {
                Text("No stations added yet.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            } else {
                myStations.forEach { host ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(host.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(host.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            
                            Switch(
                                checked = host.available,
                                onCheckedChange = { checked ->
                                    coroutineScope.launch {
                                        try {
                                            firestoreService.updateStationStatus(host.id, checked)
                                            refreshMyStations()
                                            Toast.makeText(context, "${host.name} is now ${if (checked) "Online" else "Offline"}", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error updating status", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4CAF50),
                                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Pending Booking Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (!isAvailable) {
                Text("Go Online to receive new booking requests.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            } else if (pendingBookings.isEmpty()) {
                Text("No pending requests.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            } else {
                pendingBookings.forEach { booking ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(booking.stationName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("₹${String.format("%.0f", booking.amountPaid)} • ${booking.durationHours}h", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Decline Button
                                IconButton(
                                    onClick = { 
                                        coroutineScope.launch {
                                            firestoreService.updateBookingStatus(booking.id, "Cancelled")
                                            pendingBookings = pendingBookings.filter { it.id != booking.id }
                                            Toast.makeText(context, "Request Declined", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape).size(40.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                                }
                                // Accept Button
                                IconButton(
                                    onClick = { 
                                        coroutineScope.launch {
                                            firestoreService.updateBookingStatus(booking.id, "Completed")
                                            pendingBookings = pendingBookings.filter { it.id != booking.id }
                                            todayEarnings += booking.amountPaid
                                            Toast.makeText(context, "Request Accepted!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.background(Color(0xFFE8F5E9), CircleShape).size(40.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
