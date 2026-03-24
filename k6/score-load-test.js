import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    // TPS 500 목표: 500 VU × 1 req/sec
    vus: 500,
    duration: '30m',

    thresholds: {
        http_req_duration: ['p(95)<50'], // p95 응답시간 50ms 이내
        http_req_failed: ['rate<0.005'],   // 에러율 0.5% 미만
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// VU별 고유 userId와 nickname 생성
const userIds = {};

export default function () {
    // VU별 고유 사용자 유지
    if (!userIds[__VU]) {
        userIds[__VU] = uuidv4();
    }

    const payload = JSON.stringify({
        userId: userIds[__VU],
        nickname: `player_${__VU}`,
        score: Math.floor(Math.random() * 29001) + 1000, // 1000~30000 랜덤 score
    });

    const res = http.post(`${BASE_URL}/api/rankings/score`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has rank': (r) => JSON.parse(r.body).rank > 0,
    });

    sleep(1); // 1초 간격 → VU당 1 req/sec
}
