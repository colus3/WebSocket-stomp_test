# WebSocket STOMP 멀티모듈 샘플 프로젝트

Spring Boot 멀티모듈(server + client) 구조로 구성된 WebSocket STOMP 학습용 프로젝트.
서버가 10초마다 시스템 메트릭(CPU 사용률)을 STOMP로 발행하면, 클라이언트가 구독하여 수신한다.
발행된 메트릭은 MySQL에 이력으로 저장된다.

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Kotlin | 2.2.21 |
| Spring Boot | 4.0.3 |
| Spring WebSocket (STOMP) | Spring Boot BOM |
| Spring Data JPA | Spring Boot BOM |
| MySQL | 8.0 |
| kotlin-logging-jvm | 7.0.3 |
| Java Toolchain | 21 |
| Gradle | 9.x (Kotlin DSL) |

---

## 프로젝트 구조

```
WebSocket-stomp_test/
├── settings.gradle.kts         # 멀티모듈 선언 (server, client)
├── build.gradle.kts            # 루트 공통 설정 (subprojects 블록)
├── compose.yml                 # MySQL 8.0 Docker Compose
├── gradle/                     # Gradle Wrapper
├── gradlew / gradlew.bat
│
├── server/                     # 메트릭 발행 서버 (port 8080)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/test/stomp/server/
│       │   ├── ServerApplication.kt          # @SpringBootApplication + @EnableScheduling
│       │   ├── config/
│       │   │   └── WebSocketConfig.kt        # STOMP 브로커 설정
│       │   ├── domain/
│       │   │   ├── SystemMetric.kt           # JPA Entity
│       │   │   └── SystemMetricRepository.kt # JpaRepository
│       │   └── scheduler/
│       │       └── MetricPublisher.kt        # 10초마다 메트릭 발행
│       └── resources/
│           └── application.yaml
│
└── client/                     # 메트릭 구독 클라이언트 (port 8081)
    ├── build.gradle.kts
    └── src/main/
        ├── kotlin/com/test/stomp/client/
        │   ├── ClientApplication.kt          # @SpringBootApplication
        │   ├── config/
        │   │   └── StompClientConfig.kt      # STOMP 연결 및 구독 설정
        │   └── handler/
        │       └── MetricMessageHandler.kt   # 메시지 수신 핸들러
        └── resources/
            └── application.yaml
```

---

## 아키텍처 흐름

```
┌─────────────────────────────────────────────────────────────┐
│  Server (port 8080)                                         │
│                                                             │
│  MetricPublisher (@Scheduled 10s)                           │
│    │                                                        │
│    ├─► SystemMetricRepository.save()  ──► MySQL             │
│    │                                                        │
│    └─► SimpMessagingTemplate                                │
│            .convertAndSend("/topic/metrics", metric)        │
│                     │                                       │
│              STOMP Broker (/topic)                          │
│                     │                                       │
└─────────────────────┼───────────────────────────────────────┘
                      │  WebSocket (ws://localhost:8080/ws)
┌─────────────────────┼───────────────────────────────────────┐
│  Client (port 8081) │                                       │
│                     ▼                                       │
│  StompClientConfig (connectAsync)                           │
│    └─► session.subscribe("/topic/metrics", handler)         │
│                     │                                       │
│  MetricMessageHandler.handleFrame()                         │
│    └─► log.info("[STOMP] Received metric: {}", payload)     │
└─────────────────────────────────────────────────────────────┘
```

---

## 사전 요구 사항

- JDK 21 이상
- Docker / Docker Compose

---

## 실행 방법

### 1. MySQL 기동

```bash
docker compose up -d
```

MySQL 컨테이너가 정상 기동되었는지 확인:

```bash
docker compose ps
```

### 2. 서버 실행 (터미널 1)

```bash
./gradlew :server:bootRun
```

서버가 포트 **8080**에서 기동되고, STOMP 엔드포인트 `/ws`가 활성화된다.

### 3. 클라이언트 실행 (터미널 2)

```bash
./gradlew :client:bootRun
```

클라이언트가 포트 **8081**에서 기동되고, 서버의 `/topic/metrics`를 구독한다.

---

## 동작 확인

### 서버 로그 (10초마다 출력)

```
INFO --- MetricPublisher : Saved metric id=1 type=CPU_USAGE value=42.35%
INFO --- MetricPublisher : Saved metric id=2 type=CPU_USAGE value=67.80%
INFO --- MetricPublisher : Saved metric id=3 type=CPU_USAGE value=15.22%
```

### 클라이언트 로그

```
INFO --- StompClientConfig     : [STOMP] Connected to server: ws://localhost:8080/ws
INFO --- StompClientConfig     : [STOMP] Subscribed to topic: /topic/metrics
INFO --- MetricMessageHandler  : [STOMP] Received metric: {"id":1,"metricType":"CPU_USAGE","value":42.35,"unit":"%","recordedAt":"2026-03-09T..."}
```

### MySQL 이력 확인

```bash
docker exec -it $(docker compose ps -q mysql) \
  mysql -ustomp -pstomp1234 stomp_db \
  -e "SELECT * FROM system_metric ORDER BY id DESC LIMIT 10;"
```

---

## 설정

### 서버 (`server/src/main/resources/application.yaml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stomp_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: stomp
    password: stomp1234
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### 클라이언트 (`client/src/main/resources/application.yaml`)

```yaml
server:
  port: 8081

stomp:
  server:
    url: ws://localhost:8080/ws
    topic: /topic/metrics
```

---

## 주요 구현 포인트

### STOMP 브로커 설정 (서버)

```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")          // 구독 prefix
        registry.setApplicationDestinationPrefixes("/app") // 발행 prefix
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")                    // WebSocket 연결 엔드포인트
    }
}
```

> SockJS를 사용하지 않고 네이티브 WebSocket으로 연결한다.

### 스케줄러 기반 메트릭 발행 (서버)

```kotlin
@Scheduled(fixedDelay = 10_000)
fun publishMetric() {
    val metric = SystemMetric(metricType = "CPU_USAGE", value = Random.nextDouble(5.0, 95.0), unit = "%")
    val saved = repository.save(metric)
    messagingTemplate.convertAndSend("/topic/metrics", saved)
}
```

### STOMP 클라이언트 연결 (클라이언트)

```kotlin
@PostConstruct
fun connect() {
    val stompClient = WebSocketStompClient(StandardWebSocketClient())
    stompClient.connectAsync(serverUrl, object : StompSessionHandlerAdapter() {
        override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
            session.subscribe(topic, metricMessageHandler)
        }
    })
}
```

---

## 로깅 — kotlin-logging

Java의 Lombok `@Slf4j`에 해당하는 Kotlin 관용 로깅 라이브러리로 [kotlin-logging](https://github.com/oshai/kotlin-logging)을 사용한다.

### Java와의 비교

| | Java (Lombok) | Kotlin (kotlin-logging) |
|---|---|---|
| 선언 | `@Slf4j` 클래스 어노테이션 | 파일 최상단 `private val log = KotlinLogging.logger {}` |
| 호출 | `log.info("msg {}", arg)` | `log.info { "msg $arg" }` |
| 지연 평가 | 문자열 연결은 없으나 인자는 항상 평가됨 | 람다 전체가 지연 실행 |

### 선언 방식

클래스 내부 필드 대신 **파일 최상단**에 선언해 클래스 인스턴스와 무관하게 사용한다.

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

@Component
class MetricPublisher(...) {
    fun publishMetric() {
        log.info { "Saved metric id=${saved.id} value=${saved.value}%" }
    }
}
```

### 람다 스타일의 성능 이점

`log.info { "..." }` 형태는 내부적으로 `isInfoEnabled` 체크를 먼저 수행하고,
`true`일 때만 람다를 실행해 문자열을 생성한다.
로그 레벨이 비활성화된 경우 문자열 연산 및 객체 참조가 **전혀 발생하지 않는다**.

```kotlin
// SLF4J placeholder — 인자 평가는 항상 일어남
log.info("value={}", heavyMethod())   // heavyMethod()는 레벨 무관하게 호출

// kotlin-logging 람다 — 레벨 비활성화 시 람다 자체가 실행되지 않음
log.info { "value=${heavyMethod()}" } // INFO 비활성화면 heavyMethod() 호출 안 됨
```

### 의존성

루트 `build.gradle.kts`의 `subprojects` 블록에 공통 적용되어 있다.

```kotlin
dependencies {
    "implementation"("io.github.oshai:kotlin-logging-jvm:7.0.3")
}
```

---

## 빌드

전체 모듈 빌드:

```bash
./gradlew build
```

특정 모듈만 빌드:

```bash
./gradlew :server:build
./gradlew :client:build
```