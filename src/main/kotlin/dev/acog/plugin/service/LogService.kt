package dev.acog.plugin.service

import dev.acog.plugin.config.LogDetail
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.config.TicketConfig
import dev.acog.plugin.domain.entity.Ticket
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.Color
import java.time.LocalDateTime

@Service
class LogService(
    private val jda: JDA,
    private val ticketConfig: TicketConfig,
    private val messageConfig: MessageConfig
) {
    private val logger = LoggerFactory.getLogger(LogService::class.java)

    fun logTicketCreate(ticket: Ticket) {
        val config = messageConfig.logs.ticketOpen
        sendEmbed(ticket, config, messageConfig.logLabels.labelCreatedAt)
    }

    fun logTicketClose(ticket: Ticket) {
        val config = messageConfig.logs.ticketClose
        sendEmbed(ticket, config, messageConfig.logLabels.labelClosedAt)
    }

    fun logTicketReopen(ticket: Ticket) {
        val config = messageConfig.logs.ticketReopen
        sendEmbed(ticket, config, messageConfig.logLabels.labelReopenedAt)
    }

    fun logTicketDelete(ticket: Ticket) {
        val config = messageConfig.logs.ticketDelete
        sendEmbed(ticket, config, messageConfig.logLabels.labelDeletedAt)
    }

    fun logIncome(ticket: Ticket, amount: String) {
        val config = messageConfig.logs.ticketIncome
        sendEmbed(ticket, config, messageConfig.logLabels.labelRecordedAt, extraFields = mapOf(messageConfig.logLabels.labelAmount to amount))
    }

    fun logSurvey(ticket: Ticket, rating: Int, ratingText: String) {
        val config = messageConfig.logs.ticketSurvey
        sendEmbed(ticket, config, messageConfig.logLabels.labelSubmittedAt, extraFields = mapOf(messageConfig.logLabels.labelRating to "$ratingText ($rating)"))
    }

    private fun sendEmbed(
        ticket: Ticket, 
        config: LogDetail, 
        timeLabel: String, 
        spec: String? = null,
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
                .addField("플러그인", ticket.pluginName, true)
                .addField("고객", ticket.ownerName, true)
                .addField(timeLabel, LocalDateTime.now().toString(), true)

            extraFields.forEach { (name, value) ->
                embed.addField(name, value, true)
            }

            val messageEmbed = embed.setColor(getColor(config.color))
                .setFooter("Ticket ID: ${ticket.id}")
                .build()

            val action = logChannel.sendMessageEmbeds(messageEmbed)
            
            if (spec != null) {
                action.addFiles(FileUpload.fromData(spec.toByteArray(), "technical-spec.md"))
            }

            action.queue()
            
        } catch (e: Exception) {
            logger.error("Failed to send log to Discord", e)
        }
    }

    private fun getColor(colorName: String): Color {
        return when (colorName.uppercase()) {
            "GREEN" -> Color.GREEN
            "RED" -> Color.RED
            "BLUE" -> Color.BLUE
            "ORANGE" -> Color.ORANGE
            "YELLOW" -> Color.YELLOW
            else -> try {
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
}
