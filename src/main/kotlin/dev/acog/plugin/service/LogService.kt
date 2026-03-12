package dev.acog.plugin.service

import dev.acog.plugin.config.LogDetail
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.config.TicketConfig
import dev.acog.plugin.domain.entity.Ticket
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class LogService(
    private val jda: JDA,
    private val ticketConfig: TicketConfig,
    private val messageConfig: MessageConfig
) {
    private val logger = LoggerFactory.getLogger(LogService::class.java)

    fun logTicketCreate(ticket: Ticket) {
        sendEmbed(ticket, messageConfig.logs.ticketOpen, messageConfig.logLabels.labelCreatedAt)
    }

    fun logTicketClose(ticket: Ticket) {
        sendEmbed(ticket, messageConfig.logs.ticketClose, messageConfig.logLabels.labelClosedAt)
    }

    fun logTicketReopen(ticket: Ticket) {
        sendEmbed(ticket, messageConfig.logs.ticketReopen, messageConfig.logLabels.labelReopenedAt)
    }

    fun logTicketDelete(ticket: Ticket) {
        sendEmbed(ticket, messageConfig.logs.ticketDelete, messageConfig.logLabels.labelDeletedAt)
    }

    fun logIncome(ticket: Ticket, amount: String) {
        sendEmbed(
            ticket,
            messageConfig.logs.ticketIncome,
            messageConfig.logLabels.labelRecordedAt,
            extraFields = mapOf(messageConfig.logLabels.labelAmount to amount)
        )
    }

    fun logSurvey(ticket: Ticket, rating: Int, ratingText: String) {
        sendEmbed(
            ticket,
            messageConfig.logs.ticketSurvey,
            messageConfig.logLabels.labelSubmittedAt,
            extraFields = mapOf(messageConfig.logLabels.labelRating to "$ratingText ($rating)")
        )
    }

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    private fun sendEmbed(
        ticket: Ticket,
        config: LogDetail,
        timeLabel: String,
        extraFields: Map<String, String> = emptyMap()
    ) {
        try {
            val logChannel = jda.getTextChannelById(ticketConfig.logChannelId)
            if (logChannel == null) {
                logger.warn("Log channel not found: ${ticketConfig.logChannelId}")
                return
            }

            val embed = EmbedBuilder()
                .setTitle(config.title)
                .addField(messageConfig.logLabels.labelPlugin, ticket.pluginName, true)
                .addField(messageConfig.logLabels.labelCustomer, ticket.ownerName, true)
                .addField(timeLabel, LocalDateTime.now().format(DATE_TIME_FORMATTER), true)

            extraFields.forEach { (name, value) ->
                embed.addField(name, value, true)
            }

            val messageEmbed = embed.setColor(parseColor(config.color))
                .setFooter("Ticket ID: ${ticket.id}")
                .build()

            logChannel.sendMessageEmbeds(messageEmbed).queue()
        } catch (e: Exception) {
            logger.error("Failed to send log to Discord", e)
        }
    }

    private fun parseColor(colorName: String): Color {
        return try {
            if (colorName.startsWith("#")) {
                Color.decode(colorName)
            } else {
                val field = Color::class.java.getField(colorName.uppercase())
                field.get(null) as Color
            }
        } catch (e: Exception) {
            logger.warn("Invalid color: $colorName, defaulting to GRAY")
            Color.GRAY
        }
    }
}
