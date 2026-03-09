package com.test.stomp.client.handler

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class MetricMessageHandler : StompFrameHandler {
    private val log = LoggerFactory.getLogger(MetricMessageHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type = String::class.java

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        log.info("[STOMP] Received metric: {}", payload)
    }
}
