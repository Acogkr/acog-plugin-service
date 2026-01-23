package dev.acog.plugin.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "tickets",
    indexes = [
        Index(name = "idx_owner_status", columnList = "ownerId,status"),
        Index(name = "idx_last_activity", columnList = "lastActivityAt"),
        Index(name = "idx_status", columnList = "status")
    ]
)
data class Ticket(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val channelId: String,

    @Column(nullable = false)
    val ownerId: String,

    @Column(nullable = false)
    val ownerName: String,

    @Column(nullable = false)
    val pluginName: String,

    @Column(nullable = false)
    val pluginVersion: String,

    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    var status: TicketStatus = TicketStatus.OPEN,

    @Column(nullable = false)
    var aiEnabled: Boolean = true,

    @Column(nullable = false)
    var lastActivityAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true)
    var lastNotificationAt: LocalDateTime? = null,

    @Column(nullable = false, length = 2000, columnDefinition = "VARCHAR(2000) DEFAULT ''")
    val description: String = "",

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    var hasRevenue: Boolean = false,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TicketStatus {
    OPEN, CLOSED, DELETED
}
