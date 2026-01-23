package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.AccountConfig
import dev.acog.plugin.config.BotColors
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component

@Component
class AccountCommand(
    private val accountConfig: AccountConfig
) : SlashCommand {
    override val data = Commands.slash("account", "입금 계좌 정보를 확인합니다.")

    override fun execute(event: SlashCommandInteractionEvent) {
        val embed = EmbedBuilder()
            .setTitle("💸 ${accountConfig.title}")
            .setColor(BotColors.INFO)
            .setDescription(
                """
                **🏦 은행** : ${accountConfig.bank}
                **👤 예금주** : ${accountConfig.owner}
                
                **💳 계좌번호**
                ```
                ${accountConfig.number}
                ```
                
                > 💡 ${accountConfig.description}
                """.trimIndent()
            )
            .setFooter("입금 확인을 위해 입금자명을 정확히 기재해주세요.")
            .build()

        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
}
