package com.test.stomp.server.scheduler

import com.test.stomp.server.domain.SystemMetric
import com.test.stomp.server.domain.SystemMetricRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

private val log = KotlinLogging.logger {}

@Component
class MetricPublisher(
    private val repository: SystemMetricRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    @Scheduled(fixedDelay = 10_000)
    fun publishMetric() {
        val cpuUsage = String.format("%.2f", Random.nextDouble(5.0, 95.0)).toDouble()
        val metric = SystemMetric(
            metricType = "CPU_USAGE",
            value = cpuUsage,
            unit = "%"
        )
        val saved = repository.save(metric)
        log.info { "Saved metric id=${saved.id} type=${saved.metricType} value=${saved.value}%" }
        messagingTemplate.convertAndSend("/topic/metrics", saved)
    }

    @Scheduled(fixedDelay = 7_000)
    fun publishMetricByDevice() {
        val devices = listOf("device-001", "device-002", "device-003")
        devices.forEach { deviceId ->
            val memoryUsage = String.format("%.2f", Random.nextDouble(10.0, 90.0)).toDouble()
            val metric = SystemMetric(
                metricType = "MEMORY_USAGE",
                value = memoryUsage,
                unit = "%"
            )
            val saved = repository.save(metric)
            log.info { "Saved metric id=${saved.id} device=$deviceId type=${saved.metricType} value=${saved.value}%" }
            messagingTemplate.convertAndSend("/topic/metrics/$deviceId", saved)
        }
    }
}
