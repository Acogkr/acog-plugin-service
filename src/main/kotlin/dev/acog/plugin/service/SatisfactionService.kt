package dev.acog.plugin.service

import dev.acog.plugin.domain.entity.Satisfaction
import dev.acog.plugin.domain.repository.SatisfactionRepository
import dev.acog.plugin.domain.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SatisfactionService(
    private val satisfactionRepository: SatisfactionRepository,
    private val ticketRepository: TicketRepository,
    private val logService: LogService
) {
    private val logger = LoggerFactory.getLogger(SatisfactionService::class.java)

    @Transactional
    fun saveSurveyResult(
        ticketId: Long,
        ownerId: String,
        ownerName: String,
        pluginName: String,
        rating: Int,
        ratingText: String
    ) {
        satisfactionRepository.save(
            Satisfaction(
                ticketId = ticketId,
                ownerId = ownerId,
                ownerName = ownerName,
                pluginName = pluginName,
                rating = rating
            )
        )

        ticketRepository.findById(ticketId).ifPresent { ticket ->
            logService.logSurvey(ticket, rating, ratingText)
        }

        logger.info("Satisfaction survey saved - Ticket: $ticketId, Rating: $rating ($ratingText)")
    }
}
