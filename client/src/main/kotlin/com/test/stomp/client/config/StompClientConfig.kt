package com.test.stomp.client.config

import com.test.stomp.client.handler.MetricMessageHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient

private val log = KotlinLogging.logger {}

@Configuration
class StompClientConfig(
    private val metricMessageHandler: MetricMessageHandler,
    @Value("\${stomp.server.url}") private val serverUrl: String,
    @Value("\${stomp.server.topic}") private val topic: String
) {

    @PostConstruct
    fun connect() {
        val scheduler = ThreadPoolTaskScheduler().also { it.initialize() }
        val stompClient = WebSocketStompClient(StandardWebSocketClient()).apply {
            taskScheduler = scheduler
            defaultHeartbeat = longArrayOf(10000, 10000)
        }

        stompClient.connectAsync(serverUrl, object : StompSessionHandlerAdapter() {
            override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                log.info { "[STOMP] Connected to server: $serverUrl" }
                session.subscribe(topic, metricMessageHandler)
                log.info { "[STOMP] Subscribed to topic: $topic" }
            }

            override fun handleTransportError(session: StompSession, exception: Throwable) {
                log.error { "[STOMP] Transport error: ${exception.message}" }
            }
        })
    }
}