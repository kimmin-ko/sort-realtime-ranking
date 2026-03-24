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
Game Client (k6) → POST /score
              ↓
     Redis Sorted Set (score 업데이트)
              ↓
     Redis Pub/Sub PUBLISH ──────────→ Redis Pub/Sub SUBSCRIBE
              ↓                                ↓
     SseEmitterManager                 SseEmitterManager
              ↓                                ↓
     SSE → Browser A1, A2 ...         SSE → Browser B1, B2 ...
```

- **score 증가**: 게임 클라이언트(승리, 업적 달성 등)에서 호출 — k6로 부하 시뮬레이션
- **랭킹 조회**: 웹 브라우저에서 SSE로 실시간 수신

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

### 3. k6 설치

```bash
# macOS (Homebrew)
brew install k6

# 설치 확인
k6 version
```

## 실행

```bash
git clone https://github.com/kimmin-ko/sort-realtime-ranking.git
cd sort-realtime-ranking
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 시작됩니다.
브라우저에서 `http://localhost:8080`에 접속하면 실시간 랭킹 대시보드를 볼 수 있습니다.

## 테스트 방법

### Step 1. 서버 실행

터미널 3개에서 각각 실행합니다:

```bash
# 터미널 1 — 서버A (8080)
./gradlew bootRun

# 터미널 2 — 서버B (8081)
./gradlew bootRun --args='--server.port=8081'

# 터미널 3 — 서버C (8082)
./gradlew bootRun --args='--server.port=8082'
```

Redis 인증이 필요한 경우 환경변수를 추가합니다:

```bash
REDIS_USERNAME=test REDIS_PASSWORD=test ./gradlew bootRun
```

### Step 2. 브라우저에서 대시보드 접속

각 서버의 대시보드를 브라우저 탭에서 엽니다:

- `http://localhost:8080` (서버A)
- `http://localhost:8081` (서버B)
- `http://localhost:8082` (서버C)

모든 탭에서 연결 상태가 "연결됨"으로 표시되는지 확인합니다.

### Step 3. k6로 부하 생성

k6 스크립트로 다수의 플레이어가 동시에 score를 올리는 상황을 시뮬레이션합니다. 기본 설정은 20 VU가 30분간 1 req/sec으로 요청합니다.

```bash
# 기본 실행 (서버A에 부하)
k6 run k6/score-load-test.js

# 서버B에 부하를 보내고 싶은 경우
k6 run -e BASE_URL=http://localhost:8081 k6/score-load-test.js
```

### Step 4. Pub/Sub 실시간 전파 확인

k6가 서버A(8080)에만 요청을 보내더라도, **서버B(8081)와 서버C(8082)의 대시보드에도 랭킹이 실시간 갱신**되면 Pub/Sub이 정상 동작하는 것입니다.

```
k6 → POST /score → 서버A → Redis Pub/Sub 발행
                                    ↓
              서버A 대시보드: 실시간 갱신 ✓
              서버B 대시보드: 실시간 갱신 ✓  ← Pub/Sub
              서버C 대시보드: 실시간 갱신 ✓  ← Pub/Sub
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