package dev.acog.plugin.discord.listeners

import dev.acog.plugin.config.AiConfig
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.config.TicketChatConfig
import dev.acog.plugin.domain.entity.Ticket
import dev.acog.plugin.domain.entity.TicketStatus
import dev.acog.plugin.service.AiService
import dev.acog.plugin.service.TicketService
import dev.acog.plugin.util.FileUtils
import dev.acog.plugin.util.RateLimiter
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
class TicketChatListener(
    private val ticketService: TicketService,
    private val aiService: AiService,
    private val rateLimiter: RateLimiter,
    private val aiConfig: AiConfig,
    private val messageConfig: MessageConfig,
    private val fileUtils: FileUtils
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(TicketChatListener::class.java)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisorJob)
    private val pendingResponses = ConcurrentHashMap<String, Job>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || !event.isFromGuild) {
            return
        }

        val channelId = event.channel.id
        val ticket = ticketService.getTicket(channelId) ?: return

        if (ticket.status == TicketStatus.CLOSED) {
            return
        }

        val isAdmin = event.member?.hasPermission(Permission.ADMINISTRATOR) ?: false

        if (isAdmin) {
            handleAdminMessage(channelId, ticket)
            return
        }

        if (!ticket.aiEnabled) {
            return
        }

        if (!rateLimiter.checkRateLimit(event.author.id)) {
            val remaining = rateLimiter.getRemainingRequests(event.author.id)
            event.channel.sendMessage(messageConfig.errorRateLimit.format(remaining)).queue()
            return
        }

        ticketService.updateLastActivity(channelId)
        scheduleAiResponse(event, ticket, channelId)
    }

    private fun handleAdminMessage(channelId: String, ticket: Ticket) {
        if (ticket.aiEnabled) {
            ticketService.disableAi(channelId)
        }
        pendingResponses[channelId]?.cancel()
    }

    private fun scheduleAiResponse(event: MessageReceivedEvent, ticket: Ticket, channelId: String) {
        pendingResponses[channelId]?.cancel()

        val job = scope.launch {
            try {
                event.channel.sendTyping().queue()
                delay(aiConfig.responseDelayMs.toLong())

                val messages = withContext(Dispatchers.IO) {
                    event.channel.history.retrievePast(aiConfig.maxChatHistory).complete()
                }

                val chatContext = buildChatContext(ticket, messages)
                val response = aiService.generateChatResponse(chatContext)
                event.channel.sendMessage(response).queue()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(
                    "AI response failed in channel $channelId (Ticket: ${ticket.pluginName}) " +
                    "for user ${event.author.name} at ${LocalDateTime.now()}",
                    e
                )
                event.channel.sendMessage(messageConfig.errorGeneral).queue()
            } finally {
                pendingResponses.remove(channelId)
            }
        }

        pendingResponses[channelId] = job
    }

    private suspend fun buildChatContext(ticket: Ticket, messages: List<Message>): String {
        val chatConfig = messageConfig.ticketChat

        return buildString {
            appendTicketHeader(chatConfig, ticket)
            append(chatConfig.contextHistory)
            appendMessageHistory(messages, chatConfig)
        }.take(aiConfig.maxTotalContextLength)
    }

    private fun StringBuilder.appendTicketHeader(chatConfig: TicketChatConfig, ticket: Ticket) {
        append(chatConfig.contextHeader)
        append(chatConfig.contextPlugin.format(ticket.pluginName))
        append(chatConfig.contextVersion.format(ticket.pluginVersion))
        append(chatConfig.contextCustomer.format(ticket.ownerName))
    }

    private suspend fun StringBuilder.appendMessageHistory(messages: List<Message>, chatConfig: TicketChatConfig) {
        messages.reversed().forEach { message ->
            val content = message.contentDisplay.take(aiConfig.maxMessageLength)
            append("${message.author.name}: $content\n")

            message.attachments.forEach { attachment ->
                val fileContent = withContext(Dispatchers.IO) {
                    fileUtils.downloadAttachment(attachment)
                }
                if (fileContent != null) {
                    append(chatConfig.contextAttachment.format(fileContent.take(aiConfig.maxFileContentLength)))
                }
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        supervisorJob.cancel()
    }
}
