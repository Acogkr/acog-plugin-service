package dev.acog.plugin.discord.commands

import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.service.AiService
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SummaryCommand(
    private val aiService: AiService,
    private val messageConfig: MessageConfig
) : SlashCommand {
    private val logger = LoggerFactory.getLogger(SummaryCommand::class.java)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisorJob)

    override val data = Commands.slash("summary", messageConfig.summary.description)

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        event.channel.history.retrievePast(100).queue({ messages ->
            val chatLog = messages.reversed().joinToString("\n") { message ->
                "${message.author.name}: ${message.contentDisplay}"
            }

            scope.launch {
                try {
                    val specification = aiService.generateTechnicalSpec(chatLog)
                    event.hook.sendFiles(FileUpload.fromData(specification.toByteArray(), "technical-spec.md"))
                        .setContent(messageConfig.summary.content)
                        .queue()
                } catch (e: Exception) {
                    logger.error("Failed to generate technical spec", e)
                    event.hook.sendMessage(messageConfig.errorGeneral).queue()
                }
            }
        }, { error ->
            logger.error("Failed to retrieve message history", error)
            event.hook.sendMessage(messageConfig.errorGeneral).queue()
        })
    }

    @PreDestroy
    fun shutdown() {
        supervisorJob.cancel()
    }
}
