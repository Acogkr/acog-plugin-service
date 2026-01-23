package dev.acog.plugin.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "satisfaction")
data class Satisfaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val ticketId: Long,

    @Column(nullable = false)
    val ownerId: String,

    @Column(nullable = false)
    val ownerName: String,

    @Column(nullable = false)
    val pluginName: String,

    @Column(nullable = false)
    val rating: Int,

    @Column(nullable = true, length = 1000)
    val feedback: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
