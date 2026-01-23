package dev.acog.plugin.discord.listeners

import dev.acog.plugin.config.BotColors
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.constants.BotConstants
import dev.acog.plugin.domain.entity.Satisfaction
import dev.acog.plugin.domain.repository.SatisfactionRepository
import dev.acog.plugin.domain.repository.TicketRepository
import dev.acog.plugin.service.LogService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SatisfactionSurveyListener(
    private val satisfactionRepository: SatisfactionRepository,
    private val logService: LogService,
    private val ticketRepository: TicketRepository,
    private val messageConfig: MessageConfig
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(SatisfactionSurveyListener::class.java)

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            BotConstants.BTN_SATISFACTION_5 -> handleSurvey(event, 5, "⭐⭐⭐⭐⭐ 매우 만족")
            BotConstants.BTN_SATISFACTION_3 -> handleSurvey(event, 3, "⭐⭐⭐ 보통")
            BotConstants.BTN_SATISFACTION_1 -> handleSurvey(event, 1, "⭐ 불만족")
        }
    }

    private fun handleSurvey(event: ButtonInteractionEvent, rating: Int, ratingText: String) {
        try {
            val ticketId = event.message.embeds.firstOrNull()
                ?.footer?.text?.substringAfter("Ticket ID: ")?.toLongOrNull()

            val pluginName = event.message.embeds.firstOrNull()
                ?.description?.substringAfter("\"")?.substringBefore("\"") ?: messageConfig.surveyUnknownPlugin

            if (ticketId != null) {
                satisfactionRepository.save(
                    Satisfaction(
                        ticketId = ticketId,
                        ownerId = event.user.id,
                        ownerName = event.user.name,
                        pluginName = pluginName,
                        rating = rating
                    )
                )

                val ticket = ticketRepository.findById(ticketId).orElse(null)
                if (ticket != null) {
                    logService.logSurvey(ticket, rating, ratingText)
                }

                val embed = EmbedBuilder()
                    .setTitle(messageConfig.surveyThankYouTitle)
                    .setDescription(messageConfig.surveyThankYouDesc.format(ratingText))
                    .setColor(BotColors.SUCCESS)
                    .build()

                event.replyEmbeds(embed).setEphemeral(true).queue()
                
                event.message.delete().queue()
            } else {
                event.reply(messageConfig.surveyError).setEphemeral(true).queue()
            }
        } catch (e: Exception) {
            logger.error("Failed to save satisfaction survey", e)
            event.reply(messageConfig.surveySaveError).setEphemeral(true).queue()
        }
    }
}
