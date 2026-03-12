package dev.acog.plugin.domain.repository

import dev.acog.plugin.domain.entity.Ticket
import dev.acog.plugin.domain.entity.TicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TicketRepository : JpaRepository<Ticket, Long> {
    fun findByChannelId(channelId: String): Ticket?
    fun findTopByOrderByIdDesc(): Ticket?

    @Query(
        "SELECT t FROM Ticket t WHERE t.status = :status " +
        "AND t.lastActivityAt < :inactiveSince " +
        "AND (t.lastNotificationAt IS NULL OR t.lastNotificationAt < :inactiveSince)"
    )
    fun findInactiveTickets(status: TicketStatus, inactiveSince: LocalDateTime): List<Ticket>
}
