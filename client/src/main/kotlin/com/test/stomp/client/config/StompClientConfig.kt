package com.test.stomp.client.config

import com.test.stomp.client.handler.MetricMessageHandler
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient

@Configuration
class StompClientConfig(
    private val metricMessageHandler: MetricMessageHandler,
    @Value("\${stomp.server.url}") private val serverUrl: String,
    @Value("\${stomp.server.topic}") private val topic: String
) {
    private val log = LoggerFactory.getLogger(StompClientConfig::class.java)

    @PostConstruct
    fun connect() {
        val stompClient = WebSocketStompClient(StandardWebSocketClient())

        stompClient.connectAsync(serverUrl, object : StompSessionHandlerAdapter() {
            override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                log.info("[STOMP] Connected to server: {}", serverUrl)
                session.subscribe(topic, metricMessageHandler)
                log.info("[STOMP] Subscribed to topic: {}", topic)
            }

            override fun handleTransportError(session: StompSession, exception: Throwable) {
                log.error("[STOMP] Transport error: {}", exception.message)
            }
        })
    }
}
