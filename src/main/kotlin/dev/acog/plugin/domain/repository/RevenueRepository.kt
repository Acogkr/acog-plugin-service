package dev.acog.plugin.domain.repository

import dev.acog.plugin.domain.entity.Revenue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RevenueRepository : JpaRepository<Revenue, Long> {
    fun findByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<Revenue>
}
