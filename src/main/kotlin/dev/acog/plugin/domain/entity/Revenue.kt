package dev.acog.plugin.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "revenue")
data class Revenue(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val ownerName: String,

    @Column(nullable = false)
    val pluginName: String,

    @Column(nullable = false)
    val pluginVersion: String,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
