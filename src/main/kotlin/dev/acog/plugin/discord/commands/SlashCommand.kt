package dev.acog.plugin.discord.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData

interface SlashCommand {
    val data: CommandData
    val isAdminOnly: Boolean get() = true
    fun execute(event: SlashCommandInteractionEvent)
}
