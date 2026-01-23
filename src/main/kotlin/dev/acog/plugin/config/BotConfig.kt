package dev.acog.plugin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "bot")
data class BotConfig(
    var token: String = "",
    var guildId: String = ""
)
