package com.test.stomp.client.dto

import java.time.Instant

data class MetricDto(
    val id: Long,
    val metricType: String,
    val value: Double,
    val unit: String,
    val recordedAt: Instant
)
