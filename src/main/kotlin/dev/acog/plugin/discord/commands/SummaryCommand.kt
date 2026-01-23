package dev.acog.plugin.discord.commands

import dev.acog.plugin.service.AiService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.stereotype.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Component
class SummaryCommand(
    private val aiService: AiService,
    private val messageConfig: dev.acog.plugin.config.MessageConfig
) : SlashCommand {
    override val data = Commands.slash("summary", messageConfig.summary.desc)

    override fun execute(event: SlashCommandInteractionEvent) {
        val channel = event.channel
        event.deferReply().queue()

        channel.history.retrievePast(100).queue { messages ->
            val chatLog = messages.reversed().joinToString("\n") { 
                "${it.author.name}: ${it.contentDisplay}" 
            }
            
            CoroutineScope(Dispatchers.Default).launch {
                val spec = aiService.generateTechnicalSpec(chatLog)
                event.hook.sendFiles(FileUpload.fromData(spec.toByteArray(), "technical-spec.md"))
                    .setContent(messageConfig.summary.content)
                    .queue()
            }
        }
    }
}
