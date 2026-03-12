package dev.acog.plugin.service

import dev.acog.plugin.config.BotConfig
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.config.TicketConfig
import dev.acog.plugin.domain.entity.TicketStatus
import dev.acog.plugin.domain.repository.TicketRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TicketMonitorService(
    private val ticketRepository: TicketRepository,
    private val jda: JDA,
    private val botConfig: BotConfig,
    private val ticketConfig: TicketConfig,
    private val messageConfig: MessageConfig
) {
    private val logger = LoggerFactory.getLogger(TicketMonitorService::class.java)

    @Scheduled(cron = "0 0 9 * * *")
    fun checkInactiveTickets() {
        val sevenDaysAgo = LocalDateTime.now().minusDays(7)
        val tickets = ticketRepository.findInactiveTickets(TicketStatus.OPEN, sevenDaysAgo)

        tickets.forEach { ticket ->
            try {
                val channel = jda.getTextChannelById(ticket.channelId)
                val guild = jda.getGuildById(botConfig.guildId)

                if (channel != null && guild != null) {
                    val mention = resolveAdminMention(guild)

                    channel.sendMessage(
                        messageConfig.monitor.inactiveNotification.format(
                            mention,
                            ticket.ownerName,
                            ticket.pluginName,
                            ticket.pluginVersion,
                            ticket.lastActivityAt
                        )
                    ).queue(
                        {
                            ticket.lastNotificationAt = LocalDateTime.now()
                            ticketRepository.save(ticket)
                        },
                        { error -> logger.error("Failed to send inactive notification for ticket ${ticket.id}", error) }
                    )
                }
            } catch (e: Exception) {
                logger.error("Error checking inactive ticket ${ticket.id}", e)
            }
        }

        logger.info("Inactive ticket check completed. Notified ${tickets.size} tickets.")
    }

    private fun resolveAdminMention(guild: Guild): String {
        if (ticketConfig.adminRoleId.isNotBlank()) {
            return guild.getRoleById(ticketConfig.adminRoleId)?.asMention
                ?: messageConfig.monitor.defaultAdminMention
        }

        val adminRole = guild.roles.firstOrNull { role ->
            role.permissions.contains(Permission.ADMINISTRATOR)
        }
        return adminRole?.asMention ?: messageConfig.monitor.defaultAdminMention
    }
}
