package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.BotColors
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.service.TicketService
import dev.acog.plugin.util.getTicketOrReply
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component

@Component
class ReopenCommand(
    private val ticketService: TicketService,
    private val messageConfig: MessageConfig
) : SlashCommand {
    override val data = Commands.slash("reopen", messageConfig.reopen.description)
    override val isAdminOnly = true

    override fun execute(event: SlashCommandInteractionEvent) {
        val channelId = event.channel.id
        val ticket = event.getTicketOrReply(ticketService, messageConfig.reopen.notTicketChannel) ?: return

        if (ticketService.reopenTicket(channelId)) {
            val embed = EmbedBuilder()
                .setTitle(messageConfig.reopen.successTitle)
                .setDescription(messageConfig.reopen.successDescription)
                .addField(messageConfig.reopen.fieldPlugin, ticket.pluginName, true)
                .addField(messageConfig.reopen.fieldCustomer, ticket.ownerName, true)
                .setColor(BotColors.SUCCESS)
                .build()

            event.replyEmbeds(embed).setEphemeral(true).queue()
        } else {
            event.reply(messageConfig.reopen.fail).setEphemeral(true).queue()
        }
    }
}
