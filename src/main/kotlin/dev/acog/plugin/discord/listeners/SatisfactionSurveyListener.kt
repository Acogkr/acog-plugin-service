package dev.acog.plugin.discord.listeners

import dev.acog.plugin.config.BotColors
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.constants.BotConstants
import dev.acog.plugin.service.SatisfactionService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SatisfactionSurveyListener(
    private val satisfactionService: SatisfactionService,
    private val messageConfig: MessageConfig
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(SatisfactionSurveyListener::class.java)

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val surveyRating = resolveSurveyRating(event.componentId) ?: return
        handleSurvey(event, surveyRating.first, surveyRating.second)
    }

    private fun resolveSurveyRating(componentId: String): Pair<Int, String>? {
        return when (componentId) {
            BotConstants.BUTTON_SATISFACTION_5 -> 5 to messageConfig.surveyButton5
            BotConstants.BUTTON_SATISFACTION_3 -> 3 to messageConfig.surveyButton3
            BotConstants.BUTTON_SATISFACTION_1 -> 1 to messageConfig.surveyButton1
            else -> null
        }
    }

    private fun handleSurvey(event: ButtonInteractionEvent, rating: Int, ratingText: String) {
        try {
            val ticketId = parseTicketIdFromEmbeds(event.message.embeds)
            val pluginName = parsePluginNameFromEmbeds(event.message.embeds)

            if (ticketId == null) {
                event.reply(messageConfig.surveyError).setEphemeral(true).queue()
                return
            }

            satisfactionService.saveSurveyResult(
                ticketId = ticketId,
                ownerId = event.user.id,
                ownerName = event.user.name,
                pluginName = pluginName,
                rating = rating,
                ratingText = ratingText
            )

            val thankYouEmbed = EmbedBuilder()
                .setTitle(messageConfig.surveyThankYouTitle)
                .setDescription(messageConfig.surveyThankYouDescription.format(ratingText))
                .setColor(BotColors.SUCCESS)
                .build()

            event.replyEmbeds(thankYouEmbed).setEphemeral(true).queue()
            event.message.delete().queue()
        } catch (e: Exception) {
            logger.error("Failed to save satisfaction survey", e)
            event.reply(messageConfig.surveySaveError).setEphemeral(true).queue()
        }
    }

    private fun parseTicketIdFromEmbeds(embeds: List<MessageEmbed>): Long? =
        embeds.firstOrNull()
            ?.footer?.text
            ?.substringAfter("Ticket ID: ", "")
            ?.toLongOrNull()

    private fun parsePluginNameFromEmbeds(embeds: List<MessageEmbed>): String =
        embeds.firstOrNull()
            ?.description
            ?.substringAfter("\"", "")
            ?.substringBefore("\"", "")
            ?.ifBlank { null }
            ?: messageConfig.surveyUnknownPlugin
}
