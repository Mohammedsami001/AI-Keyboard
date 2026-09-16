package com.example.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.auth.TokenStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tokenStore: TokenStore,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        tokenStore.clear()
                        onSignOut()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { /* TODO Privacy Policy Link */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Privacy Policy")
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("App Version 1.0", style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
        }
    }
}
