package com.atistudios.mondly.languages.chatbot

import android.os.Handler
import com.atistudios.mondly.languages.chatbot.listeners.EndTextToSpeechCallback

internal interface ChatEngine {

    fun onChatOpened()

    fun onUserSpeakStarted()

    fun onUserAnswered(message: String?, isTyped: Boolean)

    fun onTranslationsVisibilityChanged(areTranslationsVisible: Boolean)

    fun onAutoPlayModeChanged(isAutoPlayEnabled: Boolean)

    fun onFooterHeightChanged(footerHeight: Int)

    fun onDestroy()
}

internal class ChatEngineImpl(
    private val chatView: ChatView,
    private val chatListHelper: ChatListHelper,
    private val messagesLoader: MessagesLoader,
    private val handler: Handler
) : ChatEngine {

    companion object {
        private const val BOT_MESSAGE_LOADING_DELAY = 2000L
        private const val BOT_MESSAGE_CONTENT_LOADING_DELAY = 3500L
    }

    init {
        chatListHelper.setListUpdatedListener {
            chatView.chatUpdated(it)
        }
    }

    private var areTranslationsVisible = true

    private var isAutoPlayEnabled = true

    override fun onChatOpened() {
        loadBotMessage(true)
    }

    override fun onUserAnswered(message: String?, isTyped: Boolean) {
        val userMessage = messagesLoader.buildTestUserMessage(message, isTyped)
        if (isTyped) {
            chatListHelper.addItem(userMessage)
        } else {
            chatListHelper.updateLastItem(userMessage)
        }
        chatView.botMessageLoading()
        loadBotMessage(false)
    }

    override fun onUserSpeakStarted() {
        chatListHelper.addItem(messagesLoader.buildLoadingTestUserMessage())
    }

    override fun onTranslationsVisibilityChanged(areTranslationsVisible: Boolean) {
        this.areTranslationsVisible = areTranslationsVisible
        chatListHelper.setTranslationsVisibility(areTranslationsVisible)
    }

    override fun onAutoPlayModeChanged(isAutoPlayEnabled: Boolean) {
        this.isAutoPlayEnabled = isAutoPlayEnabled
    }

    override fun onFooterHeightChanged(footerHeight: Int) {
        chatListHelper.setFooterHeight(footerHeight)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun loadBotMessage(introAnimations: Boolean) {
        chatView.progressStateChanged(true)
        val botMessage = messagesLoader.buildTestBotMessage(areTranslationsVisible)
        handler.postDelayed({
            chatView.progressStateChanged(false)
            chatListHelper.addItem(botMessage)
        }, BOT_MESSAGE_LOADING_DELAY)
        handler.postDelayed({
            chatListHelper.updateLastItem(botMessage.copy(isLoading = false))
            if (isAutoPlayEnabled) {
                chatView.disableSpeak()
                botMessage.text?.let {
                    chatView.speak(it, speakEndListener = object : EndTextToSpeechCallback() {
                        override fun onCompleted() {
                            showSuggestions(introAnimations)
                        }

                    })
                }
            } else {
                showSuggestions(introAnimations)
            }
        }, BOT_MESSAGE_CONTENT_LOADING_DELAY)
    }

    private fun showSuggestions(introAnimations: Boolean) {
        chatView.suggestionsLoaded(messagesLoader.buildTestSuggestions(), introAnimations)
    }
}