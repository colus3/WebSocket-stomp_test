package com.test.stomp.server.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "system_metric")
class SystemMetric(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val metricType: String,

    @Column(nullable = false)
    val value: Double,

    @Column(nullable = false)
    val unit: String,

    @Column(nullable = false)
    val recordedAt: Instant = Instant.now()
)
