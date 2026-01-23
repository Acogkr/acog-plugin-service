package dev.acog.plugin.domain.repository

import dev.acog.plugin.domain.entity.Ticket
import dev.acog.plugin.domain.entity.TicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketRepository : JpaRepository<Ticket, Long> {
    fun findByChannelId(channelId: String): Ticket?
    fun findByOwnerIdAndStatus(ownerId: String, status: TicketStatus): Ticket?
    fun existsByOwnerIdAndStatus(ownerId: String, status: TicketStatus): Boolean
    fun findTopByOrderByIdDesc(): Ticket?
}
