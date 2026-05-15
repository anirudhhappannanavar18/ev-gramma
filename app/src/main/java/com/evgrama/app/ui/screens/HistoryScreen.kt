package com.evgrama.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evgrama.app.data.BookingRecord
import com.evgrama.app.data.FirestoreService
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestoreService = remember { FirestoreService() }
    var historyItems by remember { mutableStateOf<List<BookingRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var role by remember { mutableStateOf("user") }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            role = firestoreService.getUserRole(uid) ?: "user"
            historyItems = firestoreService.getBookingHistory(uid, role)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            historyItems.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No bookings yet", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    Text("Your charging sessions will appear here.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(historyItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.stationName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("${item.durationHours}h session", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "₹${String.format("%.2f", item.amountPaid)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    item.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (item.status) {
                                        "Completed" -> Color(0xFF4CAF50)
                                        "Cancelled" -> Color.Red
                                        else -> Color(0xFFFFA000)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
