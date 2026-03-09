package com.test.stomp.server.scheduler

import com.test.stomp.server.domain.SystemMetric
import com.test.stomp.server.domain.SystemMetricRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class MetricPublisher(
    private val repository: SystemMetricRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val log = LoggerFactory.getLogger(MetricPublisher::class.java)

    @Scheduled(fixedDelay = 10_000)
    fun publishMetric() {
        val cpuUsage = String.format("%.2f", Random.nextDouble(5.0, 95.0)).toDouble()
        val metric = SystemMetric(
            metricType = "CPU_USAGE",
            value = cpuUsage,
            unit = "%"
        )
        val saved = repository.save(metric)
        log.info("Saved metric id={} type={} value={}%", saved.id, saved.metricType, saved.value)
        messagingTemplate.convertAndSend("/topic/metrics", saved)
    }
}
