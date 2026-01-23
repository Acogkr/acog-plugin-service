package dev.acog.plugin.discord.listeners

import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.constants.BotConstants
import dev.acog.plugin.service.TicketService
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import org.springframework.stereotype.Component

@Component
class TicketInteractionListener(
    private val ticketService: TicketService,
    private val messageConfig: MessageConfig
) : ListenerAdapter() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId == BotConstants.BUTTON_CREATE_TICKET) {
            val nameInput = TextInput.create(BotConstants.INPUT_PLUGIN_NAME, messageConfig.modalLabelName, TextInputStyle.SHORT)
                .setPlaceholder(messageConfig.modalPlaceholderName)
                .setRequired(true)
                .build()

            val versionInput = TextInput.create(BotConstants.INPUT_PLUGIN_VERSION, messageConfig.modalLabelVersion, TextInputStyle.SHORT)
                .setPlaceholder(messageConfig.modalPlaceholderVersion)
                .setRequired(true)
                .build()

            val descInput = TextInput.create(BotConstants.INPUT_PLUGIN_DESC, messageConfig.modalLabelDesc, TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageConfig.modalPlaceholderDesc)
                .setRequired(true)
                .build()

            val modal = Modal.create(BotConstants.MODAL_TICKET, messageConfig.modalTitle)
                .addActionRow(nameInput)
                .addActionRow(versionInput)
                .addActionRow(descInput)
                .build()

            event.replyModal(modal).queue()
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId == BotConstants.MODAL_TICKET) {
            val pluginName = event.getValue(BotConstants.INPUT_PLUGIN_NAME)?.asString ?: "Unknown"
            val pluginVersion = event.getValue(BotConstants.INPUT_PLUGIN_VERSION)?.asString ?: "Unknown"
            val pluginDesc = event.getValue(BotConstants.INPUT_PLUGIN_DESC)?.asString ?: ""

            try {
                val channel = ticketService.createTicket(
                    ownerId = event.user.id,
                    ownerName = event.user.name,
                    pluginName = pluginName,
                    pluginVersion = pluginVersion,
                    description = pluginDesc
                )

                event.reply(messageConfig.ticketCreateSuccess.format(channel.asMention)).setEphemeral(true).queue()
                channel.sendMessage(messageConfig.ticketWelcome.format(event.user.asMention)).queue()
            } catch (e: dev.acog.plugin.exception.GuildNotFoundException) {
                event.reply(messageConfig.errors.guildNotFound).setEphemeral(true).queue()
            } catch (e: dev.acog.plugin.exception.CategoryNotFoundException) {
                event.reply(messageConfig.errors.categoryNotFound).setEphemeral(true).queue()
            } catch (e: dev.acog.plugin.exception.MemberNotFoundException) {
                event.reply(messageConfig.errors.memberNotFound).setEphemeral(true).queue()
            } catch (e: dev.acog.plugin.exception.ChannelCreationException) {
                event.reply(messageConfig.errors.channelCreateFail.format(e.message)).setEphemeral(true).queue()
            } catch (e: Exception) {
                event.reply(messageConfig.ticketCreateFail).setEphemeral(true).queue()
            }
        }
    }
}
