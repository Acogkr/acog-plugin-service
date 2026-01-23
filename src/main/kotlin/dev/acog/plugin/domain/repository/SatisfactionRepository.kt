package dev.acog.plugin.domain.repository

import dev.acog.plugin.domain.entity.Satisfaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SatisfactionRepository : JpaRepository<Satisfaction, Long> {
    fun findByTicketId(ticketId: Long): Satisfaction?
    fun findByOwnerId(ownerId: String): List<Satisfaction>
}
