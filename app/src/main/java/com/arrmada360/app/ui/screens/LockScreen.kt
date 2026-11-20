package com.arrmada360.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arrmada360.app.R
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.ui.AppBackground
import com.arrmada360.app.ui.AppTheme
import com.arrmada360.app.ui.GradientButton

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    AppTheme(ConfigStore.amoled) {
        AppBackground {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(120.dp))
                Text("Arrmada360 is locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Text("Unlock with your fingerprint, face or screen lock", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(28.dp))
                Icon(Icons.Filled.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(20.dp))
                GradientButton("Unlock", Modifier.fillMaxWidth(0.7f)) { onUnlock() }
            }
        }
    }
}
