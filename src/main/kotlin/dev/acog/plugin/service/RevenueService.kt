package dev.acog.plugin.service

import dev.acog.plugin.domain.entity.Revenue
import dev.acog.plugin.domain.entity.Ticket
import dev.acog.plugin.domain.repository.RevenueRepository
import dev.acog.plugin.domain.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class RevenueService(
    private val revenueRepository: RevenueRepository,
    private val ticketRepository: TicketRepository
) {
    private val logger = LoggerFactory.getLogger(RevenueService::class.java)

    @Transactional
    fun addRevenue(ticket: Ticket, amount: Long) {
        revenueRepository.save(
            Revenue(
                ownerName = ticket.ownerName,
                pluginName = ticket.pluginName,
                pluginVersion = ticket.pluginVersion,
                amount = amount
            )
        )
        ticket.hasRevenue = true
        ticketRepository.save(ticket)
        logger.info("Revenue recorded - Ticket: ${ticket.id}, Plugin: ${ticket.pluginName}, Amount: ${amount}원")
    }

    fun getMonthlySales(yearMonth: YearMonth): List<Revenue> {
        val start = yearMonth.atDay(1).atStartOfDay()
        val end = yearMonth.atEndOfMonth().atTime(23, 59, 59)
        return revenueRepository.findByCreatedAtBetween(start, end)
    }

    @Transactional
    fun deleteRevenue(id: Long): Boolean {
        if (revenueRepository.existsById(id)) {
            revenueRepository.deleteById(id)
            logger.info("Revenue deleted - ID: $id")
            return true
        }
        return false
    }
}
