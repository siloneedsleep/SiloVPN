package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatMessage(val isUser: Boolean, val text: String)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>, val role: String? = null)

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class GenerationConfig(val thinkingConfig: ThinkingConfig? = null)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(val thinkingLevel: String)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val history = mutableListOf<Content>()

    init {
        // System instruction
        history.add(
            Content(
                role = "user",
                parts = listOf(Part(text = "Bạn là Trợ lý AI Quản lý VPN thông minh. Bạn có thể phân tích cấu hình WireGuard, giúp giải quyết các vấn đề mạng, tư vấn bảo mật và cung cấp thông tin. Luôn trả lời bằng Tiếng Việt."))
            )
        )
        history.add(
            Content(
                role = "model",
                parts = listOf(Part(text = "Xin chào! Tôi là Trợ lý Silo VPN. Tôi có thể giúp gì cho bạn hôm nay?"))
            )
        )
        _messages.value = listOf(ChatMessage(false, "Xin chào! Tôi là Trợ lý Silo VPN. Tôi có thể giúp gì cho bạn hôm nay?"))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(true, text)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        history.add(Content(role = "user", parts = listOf(Part(text = text))))

        viewModelScope.launch {
            try {
                val responseText = callGeminiApi()
                history.add(Content(role = "model", parts = listOf(Part(text = responseText))))
                _messages.value = _messages.value + ChatMessage(false, responseText)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(false, "Lỗi kết nối AI: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun callGeminiApi(): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Chưa cấu hình API Key. Vui lòng thiết lập trong .env."
        }

        val request = GenerateContentRequest(
            contents = history.toList(),
            generationConfig = GenerationConfig(thinkingConfig = ThinkingConfig("HIGH"))
        )
        
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Không có phản hồi."
    }
}

@Composable
fun GeminiChatScreen(viewModel: GeminiViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hỏi Trợ lý AI...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
