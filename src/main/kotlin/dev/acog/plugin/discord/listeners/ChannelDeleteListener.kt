package dev.acog.plugin.discord.listeners

import dev.acog.plugin.domain.entity.TicketStatus
import dev.acog.plugin.domain.repository.TicketRepository
import dev.acog.plugin.service.LogService
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional



@Component
class ChannelDeleteListener(
    private val ticketRepository: TicketRepository,
    private val logService: LogService
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(ChannelDeleteListener::class.java)

    @Transactional
    override fun onChannelDelete(event: ChannelDeleteEvent) {
        val channelId = event.channel.id
        val ticket = ticketRepository.findByChannelId(channelId)

        if (ticket != null) {

            ticket.status = TicketStatus.DELETED
            ticketRepository.save(ticket)
            
            logger.warn("Ticket ${ticket.id} manually deleted. Status changed to DELETED. (Plugin: ${ticket.pluginName}, Owner: ${ticket.ownerName})")


            logService.logTicketDelete(ticket)
        }
    }
}
