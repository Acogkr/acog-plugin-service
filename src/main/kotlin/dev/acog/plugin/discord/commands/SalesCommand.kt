package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.BotColors
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.service.RevenueService
import dev.acog.plugin.util.formatCurrency
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Component
class SalesCommand(
    private val revenueService: RevenueService,
    private val messageConfig: MessageConfig
) : SlashCommand {
    override val data = Commands.slash("sales", messageConfig.sales.description)
        .addOption(OptionType.STRING, "date", messageConfig.sales.optionDate, false)

    override fun execute(event: SlashCommandInteractionEvent) {
        val dateInput = event.getOption("date")?.asString
        val yearMonth = if (dateInput != null) {
            try {
                YearMonth.parse(dateInput)
            } catch (e: Exception) {
                event.reply(messageConfig.sales.dateError).setEphemeral(true).queue()
                return
            }
        } else {
            YearMonth.now()
        }

        val revenues = revenueService.getMonthlySales(yearMonth)
        val totalAmount = revenues.sumOf { it.amount }
        val count = revenues.size

        val embed = EmbedBuilder()
            .setTitle(messageConfig.sales.title.format(yearMonth))
            .setColor(BotColors.SUCCESS)
            .addField(messageConfig.sales.totalAmount, "${totalAmount.formatCurrency()}원", true)
            .addField(messageConfig.sales.count, "${count}건", true)
            .setDescription(
                revenues.takeLast(10).reversed().joinToString("\n") { revenue ->
                    messageConfig.sales.listFormat.format(
                        revenue.id,
                        revenue.createdAt.format(DateTimeFormatter.ofPattern(messageConfig.sales.dateFormat)),
                        revenue.ownerName,
                        revenue.amount.formatCurrency()
                    )
                }
            )
            .setFooter(if (count > 10) messageConfig.sales.footer else null)
            .build()

        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
}
