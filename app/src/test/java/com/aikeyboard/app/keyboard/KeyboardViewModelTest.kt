package com.aikeyboard.app.keyboard

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.aikeyboard.app.network.AiKeyboardApi
import com.aikeyboard.app.network.ApiClient
import com.aikeyboard.app.network.dto.RewriteResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyboardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: KeyboardViewModel
    private lateinit var apiClient: ApiClient
    private lateinit var api: AiKeyboardApi
    private lateinit var foregroundAppDetector: ForegroundAppDetector
    private lateinit var voiceInputController: VoiceInputController

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk()
        apiClient = mockk {
            every { api } returns this@KeyboardViewModelTest.api
        }
        foregroundAppDetector = mockk {
            every { getCurrentPackage() } returns "com.test.app"
        }
        voiceInputController = mockk(relaxed = true)
        viewModel = KeyboardViewModel(apiClient, foregroundAppDetector, voiceInputController)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rewrite success updates state and modifies text`() = runTest {
        val mockConnection = mockk<InputConnection>(relaxed = true) {
            every { getTextBeforeCursor(1000, 0) } returns "Hello "
            every { getTextAfterCursor(1000, 0) } returns "world"
        }
        
        coEvery { api.rewrite(any()) } returns RewriteResponse("Hi universe", "Casual", 100)

        viewModel.onStartInputView(EditorInfo())
        viewModel.rewrite(mockConnection)
        
        advanceUntilIdle()

        assertTrue(viewModel.rewriteState.value is RewriteState.Success)
        val success = viewModel.rewriteState.value as RewriteState.Success
        assertEquals("Casual", success.tone)
        
        verify { mockConnection.deleteSurroundingText(6, 5) }
        verify { mockConnection.commitText("Hi universe", 1) }
    }

    @Test
    fun `rewrite failure leaves original text and shows error`() = runTest {
        val mockConnection = mockk<InputConnection>(relaxed = true) {
            every { getTextBeforeCursor(1000, 0) } returns "Hello "
            every { getTextAfterCursor(1000, 0) } returns "world"
        }
        
        coEvery { api.rewrite(any()) } throws Exception("Network error")

        viewModel.onStartInputView(EditorInfo())
        viewModel.rewrite(mockConnection)
        
        advanceUntilIdle()

        assertTrue(viewModel.rewriteState.value is RewriteState.Error)
        val error = viewModel.rewriteState.value as RewriteState.Error
        assertEquals("Failed to rewrite text", error.message)
        
        // Ensure no delete or commit was called
        verify(exactly = 0) { mockConnection.deleteSurroundingText(any(), any()) }
        verify(exactly = 0) { mockConnection.commitText(any(), any()) }
    }

    @Test
    fun `undo restores exact pre-rewrite snapshot`() = runTest {
        val mockConnection = mockk<InputConnection>(relaxed = true) {
            every { getTextBeforeCursor(1000, 0) } returns "Hello "
            every { getTextAfterCursor(1000, 0) } returns "world"
        }
        
        coEvery { api.rewrite(any()) } returns RewriteResponse("Hi universe", "Casual", 100)

        viewModel.onStartInputView(EditorInfo())
        viewModel.rewrite(mockConnection)
        
        advanceUntilIdle()

        // Now simulate the text having been replaced in the connection for undo
        every { mockConnection.getTextBeforeCursor(1000, 0) } returns "Hi "
        every { mockConnection.getTextAfterCursor(1000, 0) } returns "universe"

        viewModel.undo(mockConnection)

        // It should delete the current replaced text and commit the original "Hello world"
        verify { mockConnection.deleteSurroundingText(3, 8) }
        verify { mockConnection.commitText("Hello world", 1) }
        
        assertEquals(RewriteState.Idle, viewModel.rewriteState.value)
    }
}
