package com.example.toneprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var currentSamples by remember(sampleTexts) { mutableStateOf(sampleTexts) }
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
                OutlinedTextField(
                    value = currentSamples,
                    onValueChange = { if (it.length <= 500) currentSamples = it },
                    label = { Text("Enter a few sentences you wrote...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    supportingText = { Text("${currentSamples.length} / 500 characters") }
                )

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.updateProfile(selectedPreset, currentSamples) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}
