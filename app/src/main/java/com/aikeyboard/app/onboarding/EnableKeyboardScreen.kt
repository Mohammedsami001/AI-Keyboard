package com.aikeyboard.app.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aikeyboard.app.keyboard.AiInputMethodService

@Composable
fun EnableKeyboardScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isEnabled by remember { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(false) }

    fun checkImeStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val list = imm.enabledInputMethodList
        val myPackage = context.packageName
        
        isEnabled = list.any { it.packageName == myPackage }
        
        val defaultImeId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        isDefault = defaultImeId?.startsWith(myPackage) == true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkImeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enable AI Keyboard", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        val statusText = when {
            isDefault -> "Enabled and default! You're all set."
            isEnabled -> "Enabled — not default. Please select it as your current keyboard."
            else -> "Not enabled. Please enable AI Keyboard in settings."
        }
        Text(statusText, textAlign = TextAlign.Center, color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)

        Spacer(modifier = Modifier.height(32.dp))

        if (!isEnabled) {
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }) {
                Text("1. Enable in Settings")
            }
        } else if (!isDefault) {
            Button(onClick = {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }) {
                Text("2. Set as Default")
            }
        } else {
            Button(onClick = onDone) {
                Text("Finish Setup")
            }
        }
    }
}
