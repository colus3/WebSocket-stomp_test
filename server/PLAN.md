# Server Module Plan

## 역할
10초마다 CPU 사용률 메트릭을 생성하여 STOMP로 발행하고, MySQL에 이력을 저장하는 Spring Boot 서버.

## 포트
- **8080**

## 의존성
- `spring-boot-starter-web`
- `spring-boot-starter-websocket` (STOMP broker)
- `spring-boot-starter-data-jpa`
- `mysql-connector-j`
- `jackson-module-kotlin`
- `kotlin-reflect`
- `kotlin("plugin.jpa")`

## 주요 컴포넌트

### `ServerApplication.kt`
- `@SpringBootApplication` + `@EnableScheduling`

### `config/WebSocketConfig.kt`
- `@EnableWebSocketMessageBroker`
- In-memory simple broker: `/topic`
- App destination prefix: `/app`
- Endpoint: `/ws` (native WebSocket, SockJS 없음)

### `domain/SystemMetric.kt`
- JPA Entity, 테이블명 `system_metric`
- 필드: `id` (PK, auto-increment), `metricType`, `value`, `unit`, `recordedAt`
- Jakarta EE 10 네임스페이스 사용

### `domain/SystemMetricRepository.kt`
- `JpaRepository<SystemMetric, Long>` 인터페이스

### `scheduler/MetricPublisher.kt`
- `@Scheduled(fixedDelay = 10_000)` — 10초마다 실행
- CPU 사용률 랜덤 생성 (5.0~95.0%)
- DB 저장 후 `/topic/metrics`로 브로드캐스트

## application.yaml
```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stomp_db
    username: stomp
    password: stomp1234
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

## 예상 로그
```
Saved metric id=1 type=CPU_USAGE value=42.35%
Saved metric id=2 type=CPU_USAGE value=67.80%
```

## 실행
```bash
# MySQL 먼저 기동
docker compose up -d

# 서버 실행
./gradlew :server:bootRun
```
