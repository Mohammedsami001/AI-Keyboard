package com.aikeyboard.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onNext: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            0 -> {
                Text("Welcome to AI Keyboard", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Type naturally, and let AI rewrite your sentences to perfectly match the context.", textAlign = TextAlign.Center)
            }
            1 -> {
                Text("Smart Context", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("It detects which app you're in. Chat in WhatsApp casually, email in Gmail professionally—automatically.", textAlign = TextAlign.Center)
            }
            2 -> {
                Text("Privacy First", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your keystrokes are never logged. Raw text is only sent securely when you explicitly press 'Rewrite'.", textAlign = TextAlign.Center)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            if (step < 2) step++ else onNext()
        }) {
            Text(if (step < 2) "Next" else "Get Started")
        }
    }
}
