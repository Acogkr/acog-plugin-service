package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.service.LogService
import dev.acog.plugin.service.RevenueService
import dev.acog.plugin.service.TicketService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import dev.acog.plugin.util.getTicketOrReply
import dev.acog.plugin.util.formatCurrency

@Component
class IncomeCommand(
    private val revenueService: RevenueService,
    private val ticketService: TicketService,
    private val messageConfig: MessageConfig,
    private val logService: LogService
) : SlashCommand {
    override val data = Commands.slash("income", "현재 티켓의 수익을 기록합니다.")
        .addOption(OptionType.INTEGER, "amount", "수익 금액", true)

    override fun execute(event: SlashCommandInteractionEvent) {
        val channelId = event.channel.id
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
