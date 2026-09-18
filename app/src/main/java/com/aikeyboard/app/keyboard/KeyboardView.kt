package com.aikeyboard.app.keyboard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel,
    inputConnectionProvider: () -> InputConnection?
) {
    val rewriteState by viewModel.rewriteState.collectAsState()
    val partialResults by viewModel.voiceInputController.partialResults.collectAsState()
    val finalResult by viewModel.voiceInputController.finalResult.collectAsState()
    val error by viewModel.voiceInputController.error.collectAsState()

    LaunchedEffect(finalResult) {
        if (finalResult != null) {
            viewModel.onVoiceFinalResult(inputConnectionProvider(), finalResult!!)
            viewModel.voiceInputController.startListening() // reset or wait for next press
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Suggestion / Toolbar area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (partialResults.isNotEmpty()) {
                Text("Listening: $partialResults", modifier = Modifier.weight(1f))
            } else {
                when (rewriteState) {
                    is RewriteState.Idle -> {
                        Button(onClick = { viewModel.rewrite(inputConnectionProvider()) }) {
                            Text("✨ Rewrite")
                        }
                    }
                    is RewriteState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rewriting...")
                    }
                    is RewriteState.Success -> {
                        val tone = (rewriteState as RewriteState.Success).tone
                        Text("Tone: $tone", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { viewModel.undo(inputConnectionProvider()) }) {
                            Text("Undo")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { viewModel.rewrite(inputConnectionProvider()) }) {
                            Text("✨ Rewrite Again")
                        }
                    }
                    is RewriteState.Error -> {
                        val msg = (rewriteState as RewriteState.Error).message
                        Text(msg, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { viewModel.rewrite(inputConnectionProvider()) }) {
                            Text("✨ Retry")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    if (partialResults.isEmpty() && finalResult == null) {
                        viewModel.voiceInputController.startListening()
                    } else {
                        viewModel.voiceInputController.stopListening()
                    }
                }
            ) {
                // simple mic text for now
                Text(if (partialResults.isEmpty()) "🎤" else "⏹")
            }
        }

        var isShifted by remember { mutableStateOf(false) }
        var isSymbolMode by remember { mutableStateOf(false) }
        
        val row1 = if (isSymbolMode) listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0") else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row2 = if (isSymbolMode) listOf("@", "#", "$", "%", "&", "-", "+", "(", ")") else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row3 = if (isSymbolMode) listOf("*", "\"", "'", ":", ";", "!", "?") else listOf("z", "x", "c", "v", "b", "n", "m")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        ) {
            val rowHeight = 56.dp
            
            Row(modifier = Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.SpaceEvenly) {
                row1.forEach { key ->
                    KeyboardKey(
                        text = if (isShifted && !isSymbolMode) key.uppercase() else key,
                        modifier = Modifier.weight(1f)
                    ) {
                        inputConnectionProvider()?.commitText(if (isShifted && !isSymbolMode) key.uppercase() else key, 1)
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row2.forEach { key ->
                    KeyboardKey(
                        text = if (isShifted && !isSymbolMode) key.uppercase() else key,
                        modifier = Modifier.weight(1f)
                    ) {
                        inputConnectionProvider()?.commitText(if (isShifted && !isSymbolMode) key.uppercase() else key, 1)
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.SpaceEvenly) {
                KeyboardKey(text = if (isShifted) "⬆" else "⇧", modifier = Modifier.weight(1.5f)) {
                    isShifted = !isShifted
                }
                row3.forEach { key ->
                    KeyboardKey(
                        text = if (isShifted && !isSymbolMode) key.uppercase() else key,
                        modifier = Modifier.weight(1f)
                    ) {
                        inputConnectionProvider()?.commitText(if (isShifted && !isSymbolMode) key.uppercase() else key, 1)
                    }
                }
                KeyboardKey(text = "⌫", modifier = Modifier.weight(1.5f), isRepeating = true) {
                    inputConnectionProvider()?.deleteSurroundingText(1, 0)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.SpaceEvenly) {
                KeyboardKey(text = if (isSymbolMode) "ABC" else "?123", modifier = Modifier.weight(1.5f)) {
                    isSymbolMode = !isSymbolMode
                }
                KeyboardKey(text = ",", modifier = Modifier.weight(1f)) {
                    inputConnectionProvider()?.commitText(",", 1)
                }
                KeyboardKey(text = "Space", modifier = Modifier.weight(4f)) {
                    inputConnectionProvider()?.commitText(" ", 1)
                }
                KeyboardKey(text = ".", modifier = Modifier.weight(1f)) {
                    inputConnectionProvider()?.commitText(".", 1)
                }
                KeyboardKey(text = "↵", modifier = Modifier.weight(1.5f)) {
                    inputConnectionProvider()?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    inputConnectionProvider()?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
        }
    }
}

@Composable
fun KeyboardKey(
    text: String,
    modifier: Modifier = Modifier,
    isRepeating: Boolean = false,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .padding(2.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                if (isRepeating) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onClick() // initial click
                        val job = coroutineScope.launch {
                            delay(400) // delay before repeating starts
                            while (true) {
                                onClick()
                                delay(50) // fast repeat interval
                            }
                        }
                        waitForUpOrCancellation()
                        job.cancel()
                    }
                } else {
                    detectTapGestures(onTap = { onClick() })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
