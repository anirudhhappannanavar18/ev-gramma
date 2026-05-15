package com.evgrama.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.evgrama.app.navigation.AppNavigation
import com.evgrama.app.ui.theme.EVGramaChargeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize osmdroid configuration
        org.osmdroid.config.Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        )

        setContent {
            EVGramaChargeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
