package dev.acog.plugin.service

import dev.acog.plugin.config.AiConfig
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.domain.dto.GeminiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.ClientHttpRequestFactories
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.LocalDateTime

@Service
class AiService(
    private val aiConfig: AiConfig,
    private val messageConfig: MessageConfig
) {
    private val logger = LoggerFactory.getLogger(AiService::class.java)
    private val restClient = RestClient.builder()
        .requestFactory(
            ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(30))
            )
        )
        .build()

    suspend fun generateTechnicalSpec(chatHistory: String): String = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(
            aiConfig.prompts.technicalSpec,
            messageConfig.aiDefaults.specPromptFallback,
            chatHistory,
            "TechnicalSpec"
        )
        callGemini(prompt, "TechnicalSpec")
    }

    suspend fun generateChatResponse(chatHistory: String): String = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(
            aiConfig.prompts.chatResponse,
            messageConfig.aiDefaults.chatPromptFallback,
            chatHistory,
            "ChatResponse"
        )
        callGemini(prompt, "ChatResponse")
    }

    private fun buildPrompt(
        configuredPrompt: String,
        fallbackPrompt: String,
        chatHistory: String,
        promptType: String
    ): String {
        return if (configuredPrompt.isNotBlank()) {
            configuredPrompt.replace("{chatHistory}", chatHistory)
        } else {
            logger.warn("$promptType prompt not configured, using fallback")
            fallbackPrompt.replace("\$chatHistory", chatHistory)
        }
    }

    companion object {
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    }

    private fun callGemini(text: String, requestType: String): String {
        try {
            logger.debug("Gemini API request initiated - Type: $requestType")

            val response = restClient.post()
                .uri("$GEMINI_API_URL?key={key}", aiConfig.geminiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "contents" to listOf(
                            mapOf(
                                "parts" to listOf(
                                    mapOf("text" to text)
                                )
                            )
                        )
                    )
                )
                .retrieve()
                .body(GeminiResponse::class.java)

            val result = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: messageConfig.aiDefaults.specError

            logger.debug("Gemini API request successful - Type: $requestType")
            return result
        } catch (e: HttpClientErrorException.TooManyRequests) {
            logger.warn("Gemini API Rate Limit Exceeded - Type: $requestType")
            return messageConfig.errorAiDelay
        } catch (e: Exception) {
            logger.error(
                "Gemini API request failed - Type: $requestType, Time: ${LocalDateTime.now()}, " +
                "Error: ${e.javaClass.simpleName} - ${e.message}",
                e
            )
            return messageConfig.errorGeneral
        }
    }
}
