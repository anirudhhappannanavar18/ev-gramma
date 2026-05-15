package com.evgrama.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evgrama.app.data.BookingRecord
import com.evgrama.app.data.ChargingHost
import com.evgrama.app.data.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingScreen(
    stationId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val firestoreService = remember { FirestoreService() }
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var station by remember { mutableStateOf<ChargingHost?>(null) }
    var isBookingConfirmed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var durationHours by remember { mutableIntStateOf(1) }

    LaunchedEffect(stationId) {
        // Find the station details
        val hosts = firestoreService.getChargingHosts()
        station = hosts.find { it.id == stationId }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (station == null) {
            Text("Station not found")
            Button(onClick = onBackClick) { Text("Go Back") }
        } else if (isBookingConfirmed) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.Check,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Booking Confirmed!", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your slot at ${station?.name} is reserved.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onBackClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Return to Map")
            }
        } else {
            Text(
                text = "Confirm Booking", 
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(station?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${station?.socketType} Socket • ${station?.address}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Duration", style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (durationHours > 1) durationHours-- }) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                            Text("$durationHours Hour${if (durationHours > 1) "s" else ""}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { if (durationHours < 24) durationHours++ }) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rate", style = MaterialTheme.typography.bodyLarge)
                        Text("₹${station?.pricePerHour}/hr", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    
                    val subtotal = (station?.pricePerHour ?: 0.0) * durationHours
                    val platformFee = 2.0
                    val total = subtotal + platformFee

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyLarge)
                        Text("₹${String.format("%.2f", subtotal)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Platform Fee", style = MaterialTheme.typography.bodyLarge)
                        Text("₹${String.format("%.2f", platformFee)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("₹${String.format("%.2f", total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid ?: return@Button
                    val total = ((station?.pricePerHour ?: 0.0) * durationHours) + 2.0
                    
                    coroutineScope.launch {
                        isProcessing = true
                        val balance = firestoreService.getWalletBalance(uid)
                        if (balance < total) {
                            Toast.makeText(context, "Insufficient balance! Please top up your wallet.", Toast.LENGTH_LONG).show()
                            isProcessing = false
                            return@launch
                        }

                        val booking = BookingRecord(
                            stationId = station?.id ?: "",
                            stationName = station?.name ?: "",
                            userUid = uid,
                            hostUid = station?.hostUid ?: "",
                            amountPaid = total,
                            durationHours = durationHours,
                            date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                            status = "Pending"
                        )
                        
                        val success = firestoreService.processBooking(booking)
                        if (success) {
                            isBookingConfirmed = true
                        } else {
                            Toast.makeText(context, "Payment failed. Try again.", Toast.LENGTH_SHORT).show()
                        }
                        isProcessing = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                enabled = !isProcessing
            ) {
                Text("Pay & Confirm")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    }
}
