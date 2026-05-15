package com.evgrama.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.evgrama.app.R
import androidx.compose.ui.unit.dp

@Composable
fun CalculatorScreen(
    onBackClick: () -> Unit
) {
    var batterySizeKwh by remember { mutableFloatStateOf(3f) }
    var durationMinutes by remember { mutableFloatStateOf(30f) }

    // Rough estimation: a 15A socket provides ~3kW. 
    // Energy added = Power * Time. Range ~ 35km per kWh for 2-wheelers.
    val powerKw = 3.0f
    val energyAdded = powerKw * (durationMinutes / 60f)
    val maxEnergy = minOf(energyAdded, batterySizeKwh)
    val estimatedRange = (maxEnergy * 35).toInt()
    val progress = (maxEnergy / batterySizeKwh).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.calc_title), 
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "+$estimatedRange km", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.est_range_added), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.battery_size_label, batterySizeKwh.toInt()), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = batterySizeKwh,
                    onValueChange = { batterySizeKwh = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.duration_label, durationMinutes.toInt()), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 15f..180f,
                    steps = 11
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(stringResource(R.string.go_back))
        }
    }
}
