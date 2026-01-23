package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.service.TicketService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component

@Component
class AiToggleCommand(
    private val ticketService: TicketService,
    private val messageConfig: MessageConfig
) : SlashCommand {
    override val data = Commands.slash("ai-toggle", "AI 면접관을 켜거나 끕니다.")

    override fun execute(event: SlashCommandInteractionEvent) {
        val channelId = event.channel.id
        
        val isEnabled = ticketService.toggleAi(channelId)

        if (isEnabled == null) {
            event.reply(messageConfig.errorNotTicketChannel).setEphemeral(true).queue()
            return
        }

        val message = if (isEnabled) messageConfig.aiActivated else messageConfig.aiDeactivated
        event.reply(message).setEphemeral(true).queue()
    }
}
