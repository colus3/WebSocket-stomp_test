package com.test.stomp.server.domain

import org.springframework.data.jpa.repository.JpaRepository

interface SystemMetricRepository : JpaRepository<SystemMetric, Long>
