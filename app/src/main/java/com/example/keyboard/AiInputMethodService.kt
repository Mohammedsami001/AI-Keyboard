package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    @Inject
    lateinit var keyboardViewModel: KeyboardViewModel

    @Inject
    lateinit var foregroundAppDetector: ForegroundAppDetector

    private var composeView: ComposeView? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AiInputMethodService)
            setViewTreeViewModelStoreOwner(this@AiInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@AiInputMethodService)
            
            setContent {
                androidx.compose.material3.MaterialTheme {
                    KeyboardView(
                        viewModel = keyboardViewModel,
                        inputConnectionProvider = { currentInputConnection }
                    )
                }
            }
        }
        
        val decorView = window?.window?.decorView
        if (decorView != null) {
            decorView.setViewTreeLifecycleOwner(this@AiInputMethodService)
            decorView.setViewTreeViewModelStoreOwner(this@AiInputMethodService)
            decorView.setViewTreeSavedStateRegistryOwner(this@AiInputMethodService)
        }
        
        composeView = view
        return view
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        foregroundAppDetector.setCurrentPackage(info.packageName)
        keyboardViewModel.onStartInputView(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        keyboardViewModel.onFinishInputView()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
