package com.aikeyboard.app.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aikeyboard.app.BuildConfig
import com.aikeyboard.app.auth.TokenStore
import com.aikeyboard.app.common.components.PrimaryButton
import com.aikeyboard.app.common.components.SecondaryButton
import com.aikeyboard.app.toneprofile.ToneProfileRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tokenStore: TokenStore,
    toneProfileRepository: ToneProfileRepository,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
        ) {
            PrimaryButton(
                text = "Sign Out",
                onClick = {
                    scope.launch {
                        tokenStore.clear()
                        onSignOut()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            SecondaryButton(
                text = "Clear Tone Cache",
                onClick = {
                    scope.launch {
                        toneProfileRepository.clearCache()
                        snackbarHostState.showSnackbar("Tone cache cleared")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Privacy Policy")
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("App Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
        }
    }
}
