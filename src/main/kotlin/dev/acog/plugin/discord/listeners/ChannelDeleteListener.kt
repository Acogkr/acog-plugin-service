package dev.acog.plugin.discord.listeners

import dev.acog.plugin.service.TicketService
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class ChannelDeleteListener(
    private val ticketService: TicketService
) : ListenerAdapter() {

    override fun onChannelDelete(event: ChannelDeleteEvent) {
        ticketService.markAsDeleted(event.channel.id)
    }
}
