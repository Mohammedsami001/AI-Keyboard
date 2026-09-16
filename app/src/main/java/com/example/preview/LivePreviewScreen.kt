package com.example.preview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.keyboard.KeyboardViewModel
import com.example.keyboard.RewriteState
import com.example.network.ApiClient
import com.example.network.dto.RewriteRequest
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LivePreviewViewModel @Inject constructor(
    private val apiClient: ApiClient
) : ViewModel() {
    var text by mutableStateOf("")
    var state by mutableStateOf<RewriteState>(RewriteState.Idle)
        private set

    fun rewrite() {
        if (text.isBlank()) return
        state = RewriteState.Loading
        viewModelScope.launch {
            try {
                val res = apiClient.api.rewrite(RewriteRequest(text = text))
                text = res.correctedText
                state = RewriteState.Success(res.appliedTone)
            } catch (e: Exception) {
                state = RewriteState.Error(e.message ?: "Error rewriting")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePreviewScreen(
    viewModel: LivePreviewViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Live Preview") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = viewModel.text,
                onValueChange = { viewModel.text = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                label = { Text("Type something to test") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.rewrite() },
                enabled = viewModel.state !is RewriteState.Loading
            ) {
                if (viewModel.state is RewriteState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("✨ Rewrite")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val st = viewModel.state) {
                is RewriteState.Success -> Text("Applied tone: ${st.tone}", color = MaterialTheme.colorScheme.primary)
                is RewriteState.Error -> Text(st.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }
        }
    }
}
