package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.BotColors
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.constants.BotConstants
import dev.acog.plugin.domain.entity.Ticket
import dev.acog.plugin.service.LogService
import dev.acog.plugin.service.TicketService
import dev.acog.plugin.util.getTicketOrReply
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CloseCommand(
    private val ticketService: TicketService,
    private val messageConfig: MessageConfig,
    private val logService: LogService
) : SlashCommand {
    private val logger = LoggerFactory.getLogger(CloseCommand::class.java)
    override val data = Commands.slash("close", "티켓을 종료하고 삭제합니다.")

    override fun execute(event: SlashCommandInteractionEvent) {
        val ticket = event.getTicketOrReply(ticketService, messageConfig.errorNotTicketChannel) ?: return

        event.reply(messageConfig.closeStart).setEphemeral(true).queue()

        logService.logTicketClose(ticket)
        ticketService.closeTicket(event.channel.id)

        sendSatisfactionSurvey(event, ticket)
    }

    private fun sendSatisfactionSurvey(event: SlashCommandInteractionEvent, ticket: Ticket) {
        val owner = event.guild?.getMemberById(ticket.ownerId) ?: return

        owner.user.openPrivateChannel().queue({ privateChannel ->
            val surveyEmbed = buildSurveyEmbed(ticket)

            privateChannel.sendMessageEmbeds(surveyEmbed)
                .setActionRow(
                    Button.success(BotConstants.BUTTON_SATISFACTION_5, messageConfig.surveyButton5),
                    Button.primary(BotConstants.BUTTON_SATISFACTION_3, messageConfig.surveyButton3),
                    Button.danger(BotConstants.BUTTON_SATISFACTION_1, messageConfig.surveyButton1)
                ).queue(
                    { logger.info("Satisfaction survey sent to ${ticket.ownerName} (Ticket: ${ticket.id})") },
                    { error -> logger.warn("Failed to send survey to ${ticket.ownerName}: ${error.message}") }
                )
        }, { error ->
            logger.warn("Cannot open DM with ${ticket.ownerName}: ${error.message}")
        })
    }

    private fun buildSurveyEmbed(ticket: Ticket) =
        EmbedBuilder()
            .setTitle(messageConfig.surveyTitle)
            .setDescription(messageConfig.surveyDescription.format(ticket.pluginName))
            .setColor(BotColors.INFO)
            .setFooter("Ticket ID: ${ticket.id}")
            .build()
}
