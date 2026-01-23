package dev.acog.plugin.service

import dev.acog.plugin.config.AiConfig
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.domain.dto.GeminiResponse
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.http.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

@Service
class AiService(
    private val aiConfig: AiConfig,
    private val messageConfig: MessageConfig
) {
    private val logger = LoggerFactory.getLogger(AiService::class.java)
    private val restClient = RestClient.create()

    suspend fun generateTechnicalSpec(chatHistory: String): String = withContext(Dispatchers.IO) {
        val prompt = if (aiConfig.prompts.technicalSpec.isNotBlank()) {
            aiConfig.prompts.technicalSpec.replace("{chatHistory}", chatHistory)
        } else {
            logger.warn("Technical spec prompt not configured, using fallback")
            messageConfig.aiDefaults.specPromptFallback.replace("\$chatHistory", chatHistory)
        }
        callGemini(prompt, "TechnicalSpec")
    }

    suspend fun generateChatResponse(chatHistory: String): String = withContext(Dispatchers.IO) {
        val prompt = if (aiConfig.prompts.chatResponse.isNotBlank()) {
            aiConfig.prompts.chatResponse.replace("{chatHistory}", chatHistory)
        } else {
            logger.warn("Chat response prompt not configured, using fallback")
            messageConfig.aiDefaults.chatPromptFallback.replace("\$chatHistory", chatHistory)
        }
        callGemini(prompt, "ChatResponse")
    }

    private fun callGemini(text: String, requestType: String): String {
        try {
            logger.debug("Gemini API request initiated - Type: $requestType")
            

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${aiConfig.geminiKey}"
            
            val response = restClient.post()
                .uri(url)
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
        } catch (e: org.springframework.web.client.HttpClientErrorException.TooManyRequests) {
            logger.warn("Gemini API Rate Limit Exceeded - Type: $requestType. Clean up your billing plan or wait a bit.")
            return messageConfig.errorAiDelay
        } catch (e: Exception) {
            logger.error(
                "Gemini API request failed - Type: $requestType, " +
                "Time: ${java.time.LocalDateTime.now()}, " +
                "Error: ${e.javaClass.simpleName} - ${e.message}",
                e
            )
            return messageConfig.errorGeneral
        }
    }
}
