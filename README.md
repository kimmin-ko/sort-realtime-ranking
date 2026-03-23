# Sort Realtime Ranking

Redis Sorted Set 기반 실시간 랭킹 시스템

## 기술 스택

- Java 21
- Spring Boot 4.0.4
- Spring Data Redis
- Redis

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

## API

### 1. Score 증가

```bash
curl -X POST http://localhost:8080/api/rankings/score \
  -H "Content-Type: application/json" \
  -d '{"userId": "550e8400-e29b-41d4-a716-446655440000", "nickname": "player1", "score": 10}'
```

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "nickname": "player1",
  "score": 10,
  "rank": 1
}
```

### 2. 상위 N명 랭킹 조회

```bash
curl http://localhost:8080/api/rankings/top?size=10
```

```json
{
  "rankings": [
    { "userId": "...", "nickname": "player1", "score": 100, "rank": 1 },
    { "userId": "...", "nickname": "player2", "score": 80, "rank": 2 }
  ]
}
```

### 3. 특정 사용자 랭킹 조회

```bash
curl http://localhost:8080/api/rankings/users/550e8400-e29b-41d4-a716-446655440000
```

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "nickname": "player1",
  "score": 100,
  "rank": 1
}
```

### 4. 주변 랭킹 조회 (앞뒤 N명)

```bash
curl http://localhost:8080/api/rankings/users/550e8400-e29b-41d4-a716-446655440000/nearby?range=10
```

```json
{
  "targetUser": { "userId": "...", "nickname": "player1", "score": 100, "rank": 50 },
  "rankings": [
    { "userId": "...", "nickname": "player40", "score": 110, "rank": 40 },
    { "userId": "...", "nickname": "player1", "score": 100, "rank": 50 },
    { "userId": "...", "nickname": "player60", "score": 90, "rank": 60 }
  ]
}
```

## 빠른 테스트

Redis와 서버가 실행 중인 상태에서 순서대로 실행:

```bash
# 1. 사용자 3명 score 등록
curl -s -X POST http://localhost:8080/api/rankings/score \
  -H "Content-Type: application/json" \
  -d '{"userId": "00000000-0000-0000-0000-000000000001", "nickname": "alice", "score": 100}'

curl -s -X POST http://localhost:8080/api/rankings/score \
  -H "Content-Type: application/json" \
  -d '{"userId": "00000000-0000-0000-0000-000000000002", "nickname": "bob", "score": 200}'

curl -s -X POST http://localhost:8080/api/rankings/score \
  -H "Content-Type: application/json" \
  -d '{"userId": "00000000-0000-0000-0000-000000000003", "nickname": "charlie", "score": 150}'

# 2. 상위 랭킹 확인 (bob > charlie > alice 순)
curl -s http://localhost:8080/api/rankings/top?size=10 | python3 -m json.tool

# 3. alice score 추가 증가
curl -s -X POST http://localhost:8080/api/rankings/score \
  -H "Content-Type: application/json" \
  -d '{"userId": "00000000-0000-0000-0000-000000000001", "nickname": "alice", "score": 200}'

# 4. 랭킹 변동 확인 (alice > bob > charlie 순)
curl -s http://localhost:8080/api/rankings/top?size=10 | python3 -m json.tool
```
