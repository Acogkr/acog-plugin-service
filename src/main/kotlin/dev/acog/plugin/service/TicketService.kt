package dev.acog.plugin.service

import dev.acog.plugin.config.BotColors
import dev.acog.plugin.config.BotConfig
import dev.acog.plugin.config.TicketConfig
import dev.acog.plugin.domain.entity.Ticket
import dev.acog.plugin.domain.entity.TicketStatus
import dev.acog.plugin.domain.repository.TicketRepository
import dev.acog.plugin.exception.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val ticketConfig: TicketConfig,
    private val botConfig: BotConfig,
    private val applicationContext: ApplicationContext,
    private val logService: LogService
) {
    private val logger = LoggerFactory.getLogger(TicketService::class.java)

    private val jda: JDA by lazy { applicationContext.getBean(JDA::class.java) }

    @Synchronized
    fun createTicket(
        ownerId: String,
        ownerName: String,
        pluginName: String,
        pluginVersion: String,
        description: String
    ): TextChannel {
        val guild = jda.getGuildById(botConfig.guildId)
            ?: throw GuildNotFoundException(botConfig.guildId)

        val category = jda.getCategoryById(ticketConfig.categoryId)
            ?: throw CategoryNotFoundException(ticketConfig.categoryId)

        val member = resolveMember(guild, ownerId)
            ?: throw MemberNotFoundException(ownerId)

        val ticketNumber = getNextTicketNumber()
        val channelName = buildChannelName(ticketNumber, ownerName)

        val channel = try {
            category.createTextChannel(channelName)
                .addPermissionOverride(guild.publicRole, null, listOf(Permission.VIEW_CHANNEL))
                .addPermissionOverride(
                    member,
                    listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                    null
                )
                .complete()
        } catch (e: Exception) {
            logger.error("Failed to create channel: $channelName", e)
            throw ChannelCreationException(e.message ?: "Unknown error")
        }

        grantAdminRolePermissions(guild, channel)

        val ticket = try {
            saveTicketToDatabase(
                channelId = channel.id,
                ownerId = ownerId,
                ownerName = ownerName,
                pluginName = pluginName,
                pluginVersion = pluginVersion,
                description = description
            )
        } catch (e: Exception) {
            logger.error("Failed to save ticket to database. Deleting channel...", e)
            channel.delete().queue(null) { error -> logger.error("Failed to delete channel after DB save failure: ${channel.id}", error) }
            throw e
        }

        logService.logTicketCreate(ticket)
        logger.info("Ticket created - ID: ${ticket.id}, Owner: $ownerName, Plugin: $pluginName ($pluginVersion)")

        sendDescriptionEmbed(channel, description)

        return channel
    }

    @Transactional
    fun closeTicket(channelId: String): Boolean {
        val ticket = ticketRepository.findByChannelId(channelId) ?: return false
        val channel = jda.getTextChannelById(channelId)

        if (ticket.hasRevenue) {
            archiveTicket(ticket, channel)
        } else {
            removeTicket(ticket, channel)
        }

        return true
    }

    @Transactional
    fun reopenTicket(channelId: String): Boolean {
        val ticket = ticketRepository.findByChannelId(channelId) ?: return false

        if (ticket.status == TicketStatus.OPEN) {
            logger.warn("Attempted to reopen already open ticket: ${ticket.id}")
            return false
        }

        ticket.status = TicketStatus.OPEN
        ticket.aiEnabled = true
        ticket.lastActivityAt = LocalDateTime.now()
        ticketRepository.save(ticket)

        val channel = jda.getTextChannelById(channelId) ?: return false
        restoreTicketChannel(channel, ticket)

        logService.logTicketReopen(ticket)
        logger.info("Ticket reopened - ID: ${ticket.id}, Owner: ${ticket.ownerName}")
        return true
    }

    @Transactional
    fun markAsDeleted(channelId: String) {
        val ticket = ticketRepository.findByChannelId(channelId) ?: return
        ticket.status = TicketStatus.DELETED
        ticketRepository.save(ticket)
        logService.logTicketDelete(ticket)
        logger.warn("Ticket ${ticket.id} manually deleted. (Plugin: ${ticket.pluginName}, Owner: ${ticket.ownerName})")
    }

    fun getTicket(channelId: String): Ticket? =
        ticketRepository.findByChannelId(channelId)

    @Transactional
    fun updateLastActivity(channelId: String) {
        val ticket = ticketRepository.findByChannelId(channelId) ?: return
        ticket.lastActivityAt = LocalDateTime.now()
        ticketRepository.save(ticket)
    }

    @Transactional
    fun disableAi(channelId: String) {
        val ticket = ticketRepository.findByChannelId(channelId) ?: return
        ticket.aiEnabled = false
        ticketRepository.save(ticket)
        logger.info("AI disabled for ticket ${ticket.id}")
    }

    @Transactional
    fun toggleAi(channelId: String): Boolean? {
        val ticket = ticketRepository.findByChannelId(channelId) ?: return null
        ticket.aiEnabled = !ticket.aiEnabled
        ticketRepository.save(ticket)
        logger.info("AI toggled for ticket ${ticket.id}: ${ticket.aiEnabled}")
        return ticket.aiEnabled
    }

    private fun buildChannelName(ticketNumber: Long, ownerName: String): String {
        val sanitizedName = sanitizeOwnerName(ownerName)
        val formattedNumber = ticketNumber.toString().padStart(4, '0')
        return "${ticketConfig.channelPrefix}$formattedNumber-$sanitizedName"
    }

    private fun sanitizeOwnerName(ownerName: String): String =
        ownerName
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .take(15)

    private fun resolveMember(guild: Guild, userId: String): Member? {
        return guild.getMemberById(userId) ?: try {
            guild.retrieveMemberById(userId).complete()
        } catch (e: Exception) {
            logger.error("Member retrieval failed: $userId", e)
            null
        }
    }

    private fun grantAdminRolePermissions(guild: Guild, channel: TextChannel) {
        val adminRole = guild.getRoleById(ticketConfig.adminRoleId)
        if (adminRole != null) {
            channel.manager.putPermissionOverride(
                adminRole,
                listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL),
                null
            ).queue(null) { error -> logger.error("Failed to grant admin role permissions: ${channel.id}", error) }
        } else {
            logger.warn("Admin role not found: ${ticketConfig.adminRoleId}")
        }
    }

    private fun sendDescriptionEmbed(channel: TextChannel, description: String) {
        if (description.isBlank()) {
            return
        }

        val descriptionEmbed = EmbedBuilder()
            .setTitle("📝 기획 내용")
            .setDescription(description)
            .setColor(BotColors.INFO)
            .build()
        channel.sendMessageEmbeds(descriptionEmbed).queue(null) { error -> logger.error("Failed to send description embed: ${channel.id}", error) }
    }

    @Transactional(readOnly = true)
    fun getNextTicketNumber(): Long =
        (ticketRepository.findTopByOrderByIdDesc()?.id ?: 0) + 1

    @Transactional
    fun saveTicketToDatabase(
        channelId: String,
        ownerId: String,
        ownerName: String,
        pluginName: String,
        pluginVersion: String,
        description: String
    ): Ticket {
        return ticketRepository.save(
            Ticket(
                channelId = channelId,
                ownerId = ownerId,
                ownerName = ownerName,
                pluginName = pluginName,
                pluginVersion = pluginVersion,
                description = description,
                status = TicketStatus.OPEN
            )
        )
    }

    private fun archiveTicket(ticket: Ticket, channel: TextChannel?) {
        ticket.status = TicketStatus.CLOSED
        ticketRepository.save(ticket)

        if (channel != null) {
            val archiveCategory = jda.getCategoryById(ticketConfig.archiveCategoryId)
            if (archiveCategory != null) {
                channel.manager.setParent(archiveCategory).queue(null) { error -> logger.error("Failed to move channel to archive: ${channel.id}", error) }

                val member = resolveMember(channel.guild, ticket.ownerId)
                if (member != null) {
                    channel.manager.putPermissionOverride(
                        member,
                        listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY),
                        listOf(Permission.MESSAGE_SEND)
                    ).queue(null) { error -> logger.error("Failed to update member permissions on archive: ${channel.id}", error) }
                }

                val newName = "${ticketConfig.closedChannelPrefix}${channel.name.replace(ticketConfig.channelPrefix, "")}"
                channel.manager.setName(newName).queue(null) { error -> logger.error("Failed to rename archived channel: ${channel.id}", error) }
            } else {
                logger.warn("Archive category not found, deleting channel instead.")
                channel.delete().queue(null) { error -> logger.error("Failed to delete channel (archive fallback): ${channel.id}", error) }
            }
        }
        logger.info("Ticket archived - ID: ${ticket.id}, Owner: ${ticket.ownerName}")
    }

    private fun removeTicket(ticket: Ticket, channel: TextChannel?) {
        ticket.status = TicketStatus.DELETED
        ticketRepository.save(ticket)

        if (channel != null) {
            channel.delete().queue(null) { error -> logger.error("Failed to delete ticket channel: ${channel.id}", error) }
        }
        logService.logTicketDelete(ticket)
        logger.info("Ticket deleted (no revenue) - ID: ${ticket.id}")
    }

    private fun restoreTicketChannel(channel: TextChannel, ticket: Ticket) {
        val category = jda.getCategoryById(ticketConfig.categoryId)

        if (category != null) {
            channel.manager.setParent(category).queue(null) { error -> logger.error("Failed to move channel to active category: ${channel.id}", error) }

            val member = resolveMember(channel.guild, ticket.ownerId)
            if (member != null) {
                channel.manager.putPermissionOverride(
                    member,
                    listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                    null
                ).queue(null) { error -> logger.error("Failed to restore member permissions: ${channel.id}", error) }
            }

            val newName = channel.name.removePrefix(ticketConfig.closedChannelPrefix)
            channel.manager.setName(newName).queue(null) { error -> logger.error("Failed to rename restored channel: ${channel.id}", error) }
        }
    }
}
