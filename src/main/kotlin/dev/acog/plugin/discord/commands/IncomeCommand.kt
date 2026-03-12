package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.service.LogService
import dev.acog.plugin.service.RevenueService
import dev.acog.plugin.service.TicketService
import dev.acog.plugin.util.formatCurrency
import dev.acog.plugin.util.getTicketOrReply
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component

@Component
class IncomeCommand(
    private val revenueService: RevenueService,
    private val ticketService: TicketService,
    private val messageConfig: MessageConfig,
    private val logService: LogService
) : SlashCommand {
    override val data = Commands.slash("income", messageConfig.income.description)
        .addOption(OptionType.INTEGER, "amount", messageConfig.income.optionAmount, true)

    override fun execute(event: SlashCommandInteractionEvent) {
        val ticket = event.getTicketOrReply(ticketService, messageConfig.errorNotTicketChannel) ?: return

        val amount = event.getOption("amount")?.asLong ?: 0

        if (amount <= 0) {
            event.reply(messageConfig.errorNegativeAmount).setEphemeral(true).queue()
            return
        }

        revenueService.addRevenue(ticket, amount)

        val formattedAmount = amount.formatCurrency()
        logService.logIncome(ticket, formattedAmount)
        event.reply(messageConfig.incomeRecorded.format(ticket.ownerName, ticket.pluginName, formattedAmount)).setEphemeral(true).queue()
    }
}
