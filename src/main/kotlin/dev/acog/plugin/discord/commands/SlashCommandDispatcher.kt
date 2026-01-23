package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.config.TicketConfig
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SlashCommandDispatcher(
    private val commands: List<SlashCommand>,
    private val ticketConfig: TicketConfig,
    private val messageConfig: MessageConfig
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(SlashCommandDispatcher::class.java)

    override fun onGuildReady(event: GuildReadyEvent) {
        val commandData = commands.map { it.data }
        event.guild.updateCommands().addCommands(commandData).queue {
            logger.info("Registered ${it.size} commands in guild ${event.guild.name}")
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val command = commands.find { it.data.name == event.name }
        
        if (command == null) {
            event.reply(messageConfig.commandUnknown).setEphemeral(true).queue()
            return
        }

        if (command.isAdminOnly) {
            val isAdmin = event.member?.hasPermission(Permission.ADMINISTRATOR) == true
            val hasAdminRole = ticketConfig.adminRoleId.isNotBlank() && 
                             event.member?.roles?.any { it.id == ticketConfig.adminRoleId } == true
            
            if (!isAdmin && !hasAdminRole) {
                event.reply(messageConfig.commandAdminOnly).setEphemeral(true).queue()
                return
            }
        }

        try {
            command.execute(event)
        } catch (e: Exception) {
            logger.error("Error executing command ${event.name}", e)
            event.reply(messageConfig.commandError.format(e.message)).setEphemeral(true).queue()
        }
    }
}
