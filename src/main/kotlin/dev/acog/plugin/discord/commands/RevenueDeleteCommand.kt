package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.service.RevenueService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component

@Component
class RevenueDeleteCommand(
    private val revenueService: RevenueService,
    private val messageConfig: MessageConfig
) : SlashCommand {

    override val data = Commands.slash("income-delete", messageConfig.income.deleteDescription)
        .addOption(OptionType.INTEGER, "id", messageConfig.income.optionId, true)

    override val isAdminOnly = true

    override fun execute(event: SlashCommandInteractionEvent) {
        val id = event.getOption("id")?.asLong ?: return

        if (revenueService.deleteRevenue(id)) {
            event.reply(messageConfig.income.deleteSuccess.format(id)).setEphemeral(true).queue()
        } else {
            event.reply(messageConfig.income.deleteFail).setEphemeral(true).queue()
        }
    }
}
