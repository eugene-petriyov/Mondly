package com.atistudios.mondly.languages.chatbot.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.atistudios.mondly.languages.chatbot.R
import com.atistudios.mondly.languages.chatbot.entitites.ChatMessage
import com.atistudios.mondly.languages.chatbot.entitites.ResponseSuggestion
import com.atistudios.mondly.languages.chatbot.utilities.Speaker
import kotlinx.android.synthetic.main.activity_chatbot.*
import java.util.*


class ChatBotActivity : AppCompatActivity() {

    companion object {

        private val EXTRA_CHATBOT_LANGUAGE = "extra_chatbot_language"
        private val EXTRA_CHATBOT_THEME = "extra_chatbot_theme"
        private val TTS_RESULT_CODE = 89

        fun buildIntent(context: Context, language: Locale, theme: String): Intent {
            return Intent(context, ChatBotActivity::class.java).apply {
                putExtra(EXTRA_CHATBOT_LANGUAGE, language)
                putExtra(EXTRA_CHATBOT_THEME, theme)
            }
        }
    }

    private lateinit var chatAdapter: ChatAdapter

    private var speaker: Speaker? = null

    private lateinit var chatViewModel: ChatViewModel

    private lateinit var chatLanguage: Locale

    private var chatTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)
        chatAdapter = ChatAdapter()
        recycler_view_chat_bot.layoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
            .apply { setDrawable(getDrawable(R.drawable.divider)) }
        recycler_view_chat_bot.addItemDecoration(dividerItemDecoration)
        recycler_view_chat_bot.adapter = chatAdapter
        readIntent()
        checkTTS()
        fillAdapterWithMockData()
    }

    private fun fillAdapterWithMockData() {
        val mockData = listOf(
            ChatMessage.BotMessage("Hola!", "привет", true),
            ChatMessage.UserMessage("Hola U+1F600"),
            ChatMessage.BotMessage("Commo te lammas?", "как дела", false),
            ChatMessage.UserMessage("John"),
            ChatMessage.BotMessage("Encantanda.", "приятно познакомиться", false),
            ChatMessage.UserMessage("Hola, encantando de conocerte"),
            ChatMessage.BotMessage("Como estas?", "как дела?", false, showBotAvatar = false),
            ChatMessage.BotMessage("Es un placer.", "рад слышать", false),
            ChatMessage.Footer(resources.getDimension(R.dimen.footer_height).toInt())
        )
        chatAdapter.submitList(mockData)

        SuggestionViewBinder.bindView(
            first_suggestion as ViewGroup,
            ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
        ) {

        }
        SuggestionViewBinder.bindView(
            second_suggestion as ViewGroup,
            ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
        ) {

        }
        SuggestionViewBinder.bindView(
            third_suggestion as ViewGroup,
            ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
        ) {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TTS_RESULT_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                speaker = Speaker(this, chatLanguage)
            } else {
                startActivity(Intent().apply { action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA; })
            }
        }
    }

    private fun readIntent() {
        chatLanguage = (intent.getSerializableExtra(EXTRA_CHATBOT_LANGUAGE) as? Locale) ?: Locale.getDefault()
        chatTheme = intent.getStringExtra(EXTRA_CHATBOT_THEME)
        label_title.text = chatTheme

    }

    private fun checkTTS() {
        val check = Intent()
        check.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        startActivityForResult(check, TTS_RESULT_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        speaker?.destroy()
    }
}