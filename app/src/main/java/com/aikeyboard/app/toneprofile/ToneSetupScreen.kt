package com.aikeyboard.app.toneprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aikeyboard.app.common.components.LoadingIndicator
import com.aikeyboard.app.common.components.PrimaryButton
import com.aikeyboard.app.common.components.SecondaryButton
import kotlinx.coroutines.launch

val presets = listOf("Casual", "Professional", "Friendly", "Concise", "Formal")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToneSetupScreen(
    viewModel: ToneProfileViewModel
) {
    val presetName by viewModel.presetName.collectAsState()
    val sampleTexts by viewModel.sampleTexts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearMessage()
            }
        }
    }

    var selectedPreset by remember(presetName) { mutableStateOf(presetName) }
    
    // Parse the single string back into up to 5 entries
    var entries by remember(sampleTexts) {
        mutableStateOf(
            if (sampleTexts.isBlank()) listOf("") 
            else sampleTexts.split("\n\n").take(5)
        )
    }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Tone Profile") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
        ) {
            item {
                Text("Default Tone Preset", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedPreset,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                onClick = {
                                    selectedPreset = preset
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Sample Texts (Optional calibration)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(entries) { index, text ->
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        if (newText.length <= 500) {
                            val newEntries = entries.toMutableList()
                            newEntries[index] = newText
                            entries = newEntries
                        }
                    },
                    label = { Text("Entry ${index + 1} / 5") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    supportingText = { Text("${text.length} / 500 characters") },
                    trailingIcon = {
                        if (entries.size > 1) {
                            IconButton(onClick = {
                                val newEntries = entries.toMutableList()
                                newEntries.removeAt(index)
                                entries = newEntries
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                if (entries.size < 5) {
                    SecondaryButton(
                        text = "Add Entry",
                        onClick = {
                            val newEntries = entries.toMutableList()
                            newEntries.add("")
                            entries = newEntries
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    PrimaryButton(
                        text = "Save Profile",
                        onClick = { 
                            val combined = entries.filter { it.isNotBlank() }.joinToString("\n\n")
                            viewModel.updateProfile(selectedPreset, combined)
                        }
                    )
                }
            }
        }
    }
}
