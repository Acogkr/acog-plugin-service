package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.MessageConfig
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class ReloadCommand(
    private val messageConfig: MessageConfig
) : SlashCommand {
    override val data = Commands.slash("reload", messageConfig.reload.description)

    override fun execute(event: SlashCommandInteractionEvent) {
        event.reply(messageConfig.reload.message).setEphemeral(true).queue {
            exitProcess(0)
        }
    }
}
