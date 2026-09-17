package com.aikeyboard.app.apprules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aikeyboard.app.toneprofile.presets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRulesScreen(
    viewModel: AppRulesViewModel
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val rules by viewModel.rules.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("App Rules") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(installedApps) { app ->
                val rule = rules.find { it.appPackageName == app.packageName }
                AppRuleItem(
                    app = app,
                    currentTone = rule?.toneOverride ?: "Default",
                    onToneSelected = { newTone ->
                        if (newTone == "Default") {
                            viewModel.deleteRule(app.packageName)
                        } else {
                            viewModel.updateRule(app.packageName, newTone)
                        }
                    }
                )
                Divider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRuleItem(
    app: AppInfo,
    currentTone: String,
    onToneSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Default") + presets

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentTone,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().width(140.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onToneSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
