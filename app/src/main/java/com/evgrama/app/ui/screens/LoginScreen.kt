package com.evgrama.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evgrama.app.R
import com.evgrama.app.data.FirestoreService
import com.evgrama.app.ui.components.LanguageSelector
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginAsUser: () -> Unit,
    onLoginAsHost: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoading by remember { mutableStateOf(false) }
    var userAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
    
    val coroutineScope = rememberCoroutineScope()
    val firestoreService = remember { FirestoreService() }

    // Configure Google Sign In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) 
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                isLoading = false
                if (authTask.isSuccessful) {
                    userAuthenticated = true
                } else {
                    Toast.makeText(context, "Firebase Auth Failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun navigateWithRole(role: String) {
        if (role == "user") onLoginAsUser() else onLoginAsHost()
    }

    // Auto-redirect removed to allow manual role selection for testing
    /*
    LaunchedEffect(auth.currentUser) {
        ...
    }
    */

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LanguageSelector()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.welcome_msg),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (!userAuthenticated) {
                Button(
                    onClick = {
                        isLoading = true
                        launcher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.sign_in_google), fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { userAuthenticated = true }) {
                    Text("Debug: Skip to Role Selection", color = Color.LightGray)
                }
            } else {
                Text(stringResource(R.string.select_role), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                auth.currentUser?.uid?.let { firestoreService.saveUserProfile(it, "user") }
                            } catch (e: Exception) {
                                // Fallback: allow entry even if Firestore fails for now
                            }
                            onLoginAsUser()
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { 
                    Text(stringResource(R.string.continue_user)) 
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                auth.currentUser?.uid?.let { firestoreService.saveUserProfile(it, "host") }
                            } catch (e: Exception) {
                                // Fallback
                            }
                            onLoginAsHost()
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { 
                    Text(stringResource(R.string.continue_host)) 
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(onClick = { 
                    auth.signOut()
                    googleSignInClient.signOut()
                    userAuthenticated = false 
                }) {
                    Text("Sign Out", color = Color.Gray)
                }
            }
        }
    }
}
