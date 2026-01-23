package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.BotColors
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.constants.BotConstants
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component

@Component
class SetupCommand(
    private val messageConfig: MessageConfig
) : SlashCommand {
    override val data = Commands.slash("setup", "티켓 패널을 배포합니다.")
    override val isAdminOnly = true

    override fun execute(event: SlashCommandInteractionEvent) {
        val embed = EmbedBuilder()
            .setTitle(messageConfig.setupTitle)
            .setDescription(messageConfig.setupContent)
            .setColor(BotColors.INFO)
            .build()

        val button = Button.primary(BotConstants.BUTTON_CREATE_TICKET, messageConfig.setupButtonLabel)

        event.channel.sendMessageEmbeds(embed)
            .setActionRow(button)
            .queue()
        
        event.reply(messageConfig.setupComplete).setEphemeral(true).queue()
    }
}
