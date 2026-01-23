package dev.acog.plugin.discord.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class ReloadCommand : SlashCommand {
    override val data = Commands.slash("reload", "봇을 재시작하여 설정을 갱신합니다.")

    override fun execute(event: SlashCommandInteractionEvent) {
        event.reply("봇을 재시작합니다... (약 5-10초 소요)").setEphemeral(true).queue {
            exitProcess(0)
        }
    }
}
