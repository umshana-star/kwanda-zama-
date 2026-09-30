package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatViewModelTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testChatViewModelCreationViaAndroidViewModelFactory() {
        // This is what default viewModel() calls in Jetpack Compose when given an AndroidViewModel.
        // It must find a constructor matching (Application.class) without throwing NoSuchMethodException.
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val viewModel = factory.create(ChatViewModel::class.java)

        assertNotNull(viewModel)
        assertNotNull(viewModel.messages)
    }

    @Test
    fun testChatViewModelCreationViaCustomFactory() {
        val factory = ChatViewModel.Factory(app)
        val viewModel = factory.create(ChatViewModel::class.java)

        assertNotNull(viewModel)
        assertNotNull(viewModel.messages)
    }

    @Test
    fun testChatHistoryViewModelCreationViaAndroidViewModelFactory() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val viewModel = factory.create(ChatHistoryViewModel::class.java)

        assertNotNull(viewModel)
        assertNotNull(viewModel.chatLogs)
    }
}
