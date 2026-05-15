package com.evgrama.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.evgrama.app.R
import androidx.compose.ui.unit.dp
import com.evgrama.app.data.BookingRecord
import com.evgrama.app.data.FirestoreService
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestoreService = remember { FirestoreService() }
    var userName by remember { mutableStateOf("") }
    var recentBookings by remember { mutableStateOf<List<BookingRecord>>(emptyList()) }
    var walletBalance by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val profile = firestoreService.getUserProfile(uid)
        userName = profile?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "User"
        walletBalance = profile?.walletBalance ?: 0.0
        recentBookings = firestoreService.getBookingHistory(uid, "user").take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    if (userName.isNotBlank()) "Hello, $userName 👋" else stringResource(R.string.user_greeting),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.user_subtext), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onNavigateToProfile) {
                Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wallet balance quick view
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            onClick = onNavigateToWallet
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Wallet Balance", style = MaterialTheme.typography.labelMedium)
                    Text("₹${String.format("%.2f", walletBalance)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(
            onClick = { /* In a real app, update Firestore role too */ onNavigateToProfile() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Switch to Host Mode →", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = stringResource(R.string.find_charger),
                icon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMap
            )
            DashboardCard(
                title = stringResource(R.string.calculator),
                icon = Icons.Default.Info,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCalculator
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = stringResource(R.string.wallet_label),
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToWallet
            )
            DashboardCard(
                title = stringResource(R.string.history_label),
                icon = Icons.Default.List,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToHistory
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.recent_activity), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (recentBookings.isEmpty()) {
            Text("No recent activity yet.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentBookings.size) { index ->
                    val booking = recentBookings[index]
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(booking.stationName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(booking.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text("-₹${String.format("%.2f", booking.amountPaid)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
