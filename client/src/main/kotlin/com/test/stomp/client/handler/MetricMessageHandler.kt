package com.test.stomp.client.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

private val log = KotlinLogging.logger {}

@Component
class MetricMessageHandler : StompFrameHandler {

    override fun getPayloadType(headers: StompHeaders): Type = String::class.java

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        log.info { "[STOMP] Received metric: $payload" }
    }
}
