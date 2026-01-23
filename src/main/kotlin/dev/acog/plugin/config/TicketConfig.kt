package dev.acog.plugin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "ticket")
data class TicketConfig(
    var categoryId: String = "",
    var archiveCategoryId: String = "",
    var logChannelId: String = "",
    var adminRoleId: String = "",
    var channelPrefix: String = "ticket-",
    var closedChannelPrefix: String = "closed-"
)
