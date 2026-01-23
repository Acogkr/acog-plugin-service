package dev.acog.plugin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "ai")
data class AiConfig(
    var geminiKey: String = "",
    var responseDelayMs: Int = 10000,
    var maxChatHistory: Int = 20,
    var maxMessageLength: Int = 500,
    var maxFileContentLength: Int = 2000,
    var maxTotalContextLength: Int = 8000,
    var prompts: AiPrompts = AiPrompts()
)

data class AiPrompts(
    var technicalSpec: String = "",
    var chatResponse: String = ""
)
