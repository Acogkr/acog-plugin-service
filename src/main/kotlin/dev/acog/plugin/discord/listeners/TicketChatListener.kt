package dev.acog.plugin.discord.listeners

import dev.acog.plugin.config.AiConfig
import dev.acog.plugin.config.MessageConfig
import dev.acog.plugin.domain.entity.TicketStatus
import dev.acog.plugin.service.AiService
import dev.acog.plugin.service.TicketService
import dev.acog.plugin.util.FileUtils
import dev.acog.plugin.util.RateLimiter
import kotlinx.coroutines.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
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
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pendingResponses = ConcurrentHashMap<String, Job>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (!event.isFromGuild) return

        val channelId = event.channel.id
        val ticket = ticketService.getTicket(channelId) ?: return

        if (ticket.status == TicketStatus.CLOSED) return

        val isAdmin = event.member?.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) ?: false

        if (isAdmin) {
            if (ticket.aiEnabled) {
                ticketService.disableAi(channelId)
            }
            pendingResponses[channelId]?.cancel()
            return
        }

        if (!ticket.aiEnabled) return

        if (!rateLimiter.checkRateLimit(event.author.id)) {
            val remaining = rateLimiter.getRemainingRequests(event.author.id)
            event.channel.sendMessage(messageConfig.errorRateLimit.format(remaining)).queue()
            return
        }

        ticketService.updateLastActivity(channelId)

        pendingResponses[channelId]?.cancel()

        val job = scope.launch {
            try {
                event.channel.sendTyping().queue()
                delay(aiConfig.responseDelayMs.toLong())

                val messages = withContext(Dispatchers.IO) {
                    event.channel.history.retrievePast(aiConfig.maxChatHistory).complete()
                }

                val chatLog = buildString {
                    append(messageConfig.ticketChat.logHeader)
                    append(messageConfig.ticketChat.logPlugin.format(ticket.pluginName))
                    append(messageConfig.ticketChat.logVersion.format(ticket.pluginVersion))
                    append(messageConfig.ticketChat.logCustomer.format(ticket.ownerName))
                    append(messageConfig.ticketChat.logHistory)

                    messages.reversed().forEach { msg ->
                        val content = msg.contentDisplay.take(aiConfig.maxMessageLength)
                        append("${msg.author.name}: $content\n")

                        msg.attachments.forEach { attachment ->
                            val fileContent = withContext(Dispatchers.IO) {
                                fileUtils.downloadAttachment(attachment)
                            }
                            if (fileContent != null) {
                                append(messageConfig.ticketChat.logAttachment.format(fileContent.take(aiConfig.maxFileContentLength)))
                            }
                        }
                    }
                }.take(aiConfig.maxTotalContextLength)

                val response = aiService.generateChatResponse(chatLog)
                event.channel.sendMessage(response).queue()
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                logger.error(
                    "AI response failed in channel $channelId (Ticket: ${ticket.pluginName}) " +
                    "for user ${event.author.name} at ${java.time.LocalDateTime.now()}",
                    e
                )
                event.channel.sendMessage(messageConfig.errorGeneral).queue()
            } finally {
                pendingResponses.remove(channelId)
            }
        }

        pendingResponses[channelId] = job
    }
}
