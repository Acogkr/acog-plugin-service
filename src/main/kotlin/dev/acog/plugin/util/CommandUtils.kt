package dev.acog.plugin.util

import dev.acog.plugin.domain.entity.Ticket
import dev.acog.plugin.service.TicketService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.text.NumberFormat
import java.util.Locale

fun SlashCommandInteractionEvent.getTicketOrReply(
    ticketService: TicketService,
    errorMessage: String
): Ticket? {
    val ticket = ticketService.getTicket(channel.id)
    if (ticket == null) {
        reply(errorMessage).setEphemeral(true).queue()
        return null
    }
    return ticket
}

fun Long.formatCurrency(): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(this)
