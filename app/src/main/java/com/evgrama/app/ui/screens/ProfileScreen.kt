package com.evgrama.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evgrama.app.data.FirestoreService
import com.evgrama.app.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestoreService = remember { FirestoreService() }
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var batteryCapacity by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var walletBalance by remember { mutableStateOf(0.0) }

    // Load profile from Firestore on open
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val profile = firestoreService.getUserProfile(uid)
            displayName = profile?.displayName ?: auth.currentUser?.displayName ?: ""
            vehicleModel = profile?.vehicleModel ?: ""
            batteryCapacity = profile?.batteryCapacity ?: ""
            role = profile?.role ?: ""
            walletBalance = profile?.walletBalance ?: 0.0
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(auth.currentUser?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                if (role.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(onClick = {}, label = { Text(role.replaceFirstChar { it.uppercase() }) })
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (role != "host") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Vehicle Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it },
                        label = { Text("Vehicle Model") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Ather 450X, Ola S1 Pro") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = batteryCapacity,
                        onValueChange = { batteryCapacity = it },
                        label = { Text("Battery Capacity (kWh)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 3.7") }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (isSaving) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            val uid = auth.currentUser?.uid ?: return@Button
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    firestoreService.updateUserProfile(
                                        UserProfile(
                                            uid = uid,
                                            role = role,
                                            displayName = displayName,
                                            vehicleModel = vehicleModel,
                                            batteryCapacity = batteryCapacity,
                                            walletBalance = walletBalance
                                        )
                                    )
                                    Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Save Changes")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        auth.signOut()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Logout")
                }
            }
        }
    }
}
