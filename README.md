# Sort Realtime Ranking

Redis Sorted Set 기반 실시간 랭킹 시스템. Redis Pub/Sub + SSE로 다중 서버 환경에서도 모든 브라우저에 랭킹 변동을 실시간 전파합니다.

## 기술 스택

- Java 21
- Spring Boot 4.0.4
- Spring Data Redis
- Redis (Sorted Set + Pub/Sub)
- SSE (Server-Sent Events)

## 아키텍처

```
[서버 A :8080]                          [서버 B :8081]
Browser → POST /score
              ↓
     Redis Sorted Set (score 업데이트)
              ↓
     Redis Pub/Sub PUBLISH ──────────→ Redis Pub/Sub SUBSCRIBE
              ↓                                ↓
     SseEmitterManager                 SseEmitterManager
              ↓                                ↓
     SSE → Browser A1, A2 ...         SSE → Browser B1, B2 ...
```

## 사전 준비

### 1. Java 21 설치

```bash
# macOS (Homebrew)
brew install --cask corretto@21

# 설치 확인
java -version
```

### 2. Redis 설치 및 실행

```bash
# macOS (Homebrew)
brew install redis
brew services start redis

# Docker
docker run -d --name redis -p 6379:6379 redis:latest

# 연결 확인
redis-cli ping
# PONG
```

## 실행

```bash
git clone https://github.com/kimmin-ko/sort-realtime-ranking.git
cd sort-realtime-ranking
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 시작됩니다.
브라우저에서 `http://localhost:8080`에 접속하면 실시간 랭킹 대시보드를 볼 수 있습니다.

## 다중 서버 테스트 (Pub/Sub 동작 확인)

Pub/Sub의 핵심은 **서버A에서 발생한 score 변경이 서버B에 연결된 브라우저에도 실시간 전파**되는 것입니다.

### 1. 서버 2대 실행

터미널 2개에서 각각 실행합니다:

```bash
# 터미널 1 — 서버A (8080)
./gradlew bootRun

# 터미널 2 — 서버B (8081)
./gradlew bootRun --args='--server.port=8081'
```

### 2. 브라우저에서 SSE 연결

- 브라우저 탭 1: `http://localhost:8080` (서버A 대시보드)
- 브라우저 탭 2: `http://localhost:8081` (서버B 대시보드)

두 탭 모두 연결 상태가 "연결됨"으로 표시되는지 확인합니다.

### 3. 한쪽에서 점수 추가

**서버A(8080)** 대시보드에서 닉네임과 점수를 입력하고 전송합니다.

### 4. 양쪽 실시간 반영 확인

**서버B(8081)** 대시보드에도 랭킹이 즉시 갱신되면 Pub/Sub이 정상 동작하는 것입니다.

```
서버A에서 점수 추가 → Redis Pub/Sub 발행
                         ↓
서버A 대시보드: 실시간 갱신 ✓
서버B 대시보드: 실시간 갱신 ✓  ← Pub/Sub 덕분에 가능
```

## API

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `POST` | `/api/rankings/score` | score 증가 (`{ userId, nickname, score }`) |
| `GET` | `/api/rankings/top?size=10` | 상위 N명 랭킹 조회 |
| `GET` | `/api/rankings/users/{userId}` | 특정 사용자 랭킹 조회 |
| `GET` | `/api/rankings/users/{userId}/nearby?range=10` | 주변 랭킹 조회 |
| `GET` | `/api/rankings/stream` | SSE 실시간 랭킹 스트림 |

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `REDIS_PORT` | `6379` | Redis 포트 |
| `REDIS_USERNAME` | (없음) | Redis 사용자명 |
| `REDIS_PASSWORD` | (없음) | Redis 비밀번호 |