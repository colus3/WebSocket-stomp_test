# Client Module Plan

## 역할
서버의 `/topic/metrics`를 STOMP로 구독하여 수신된 메트릭을 로그로 출력하는 Spring Boot 클라이언트.

## 포트
- **8081**

## 의존성
- `spring-boot-starter-web`
- `spring-boot-starter-websocket` (STOMP client)
- `jackson-module-kotlin`
- `kotlin-reflect`

## 주요 컴포넌트

### `ClientApplication.kt`
- `@SpringBootApplication`

### `dto/MetricDto.kt`
- 서버가 발행하는 JSON 구조에 대응하는 수신용 DTO
- `data class` → `toString()` 자동 생성으로 로그 출력에 활용
- 필드: `id`, `metricType`, `value`, `unit`, `recordedAt`

### `handler/MetricMessageHandler.kt`
- `StompFrameHandler` 구현
- `getPayloadType` → `MetricDto::class.java`
- `JacksonJsonMessageConverter`가 JSON → `MetricDto` 자동 역직렬화
- `handleFrame` → `log.info { "[STOMP] Received metric: $metric" }` 로그 출력

### `config/StompClientConfig.kt`
- `@Configuration` + `@PostConstruct`
- `WebSocketStompClient(StandardWebSocketClient())` 생성
- `JacksonJsonMessageConverter` 설정 — `application/json` 페이로드 역직렬화
- `ThreadPoolTaskScheduler` 설정 — heartbeat 처리용
- `defaultHeartbeat = [10000, 10000]` — 연결 유지
- `connectAsync` 호출 → `afterConnected`에서 `/topic/metrics` 구독
- `handleException` 오버라이드 — 프레임 처리 오류 로깅
- `application.yaml`의 `stomp.server.url`, `stomp.server.topic` 값 주입

## 메시지 처리 흐름

```
STOMP MESSAGE 프레임 수신 (content-type: application/json)
    └─► JacksonJsonMessageConverter
            └─► JSON → MetricDto 역직렬화
                    └─► MetricMessageHandler.handleFrame(payload: MetricDto)
                            └─► log.info { "$metric" }  (data class toString())
```

## application.yaml
```yaml
server:
  port: 8081
stomp:
  server:
    url: ws://localhost:8080/ws
    topic: /topic/metrics
```

## 예상 로그
```
[STOMP] Connected to server: ws://localhost:8080/ws
[STOMP] Subscribed to topic: /topic/metrics
[STOMP] Received metric: MetricDto(id=1, metricType=CPU_USAGE, value=42.35, unit=%, recordedAt=2026-03-10T...)
```

## 실행 순서
1. MySQL 기동: `docker compose up -d`
2. 서버 기동: `./gradlew :server:bootRun` (포트 8080)
3. 클라이언트 기동: `./gradlew :client:bootRun` (포트 8081, 별도 터미널)

## 검증
클라이언트 로그에서 10초마다 메트릭 수신 확인:
```
[STOMP] Received metric: MetricDto(id=N, metricType=CPU_USAGE, value=XX.XX, unit=%, recordedAt=...)
```
