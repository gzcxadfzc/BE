import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom metrics
const completeBookDuration = new Trend('complete_book_duration', true);
const initBookDuration = new Trend('init_book_duration', true);
const errorRate = new Rate('error_rate');
const completedBooks = new Counter('completed_books');

export const options = {
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },   // 워밍업
                { duration: '1m',  target: 30 },   // 부하 증가
                { duration: '2m',  target: 30 },   // 안정 구간
                { duration: '30s', target: 0 },    // 쿨다운
            ],
        },
    },
    thresholds: {
        'complete_book_duration': ['p(95)<2000', 'p(99)<5000'],
        'error_rate': ['rate<0.05'],
        'http_req_failed': ['rate<0.05'],
    },
};

// VU별 상태 (모듈 레벨 = VU당 독립 메모리)
let token = null;
let characterId = null;

function signup() {
    const username = `load_vu${__VU}_${Date.now()}`;
    const res = http.post(
        `${BASE_URL}/api/v1/auth/signup`,
        JSON.stringify({ username, password: 'loadtest123!' }),
        { headers: { 'Content-Type': 'application/json' }, tags: { name: 'signup' } }
    );
    check(res, { 'signup 200': (r) => r.status === 200 });
    return res.json('data.accessToken');
}

function createCharacter(tok) {
    const res = http.post(
        `${BASE_URL}/api/v1/character/create`,
        JSON.stringify({
            name: `캐릭터_vu${__VU}`,
            personality: '밝고 용감한',
            userDescription: '부하테스트용 캐릭터',
            appearanceDescription: '파란 눈의 소년',
        }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` }, tags: { name: 'createCharacter' } }
    );
    check(res, { 'createCharacter 200': (r) => r.status === 200 });
    return res.json('data.id');
}

function initBook(tok, charId) {
    const start = Date.now();
    const res = http.post(
        `${BASE_URL}/api/v1/book/progress/init`,
        JSON.stringify({
            characterId: charId,
            backgroundInfo: '마법의 숲',
            userInput: '주인공이 숲에 들어갔어요',
        }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` }, tags: { name: 'initBook' } }
    );
    initBookDuration.add(Date.now() - start);
    const ok = check(res, { 'initBook 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) return null;
    return res.json('data.id');
}

function completeBook(tok, bipId) {
    const start = Date.now();
    const res = http.post(
        `${BASE_URL}/api/v1/book/progress/${bipId}/complete`,
        JSON.stringify({ title: `부하테스트 동화_${bipId}`, author: `VU${__VU}` }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` }, tags: { name: 'completeBook' } }
    );
    completeBookDuration.add(Date.now() - start);
    const ok = check(res, { 'completeBook 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (ok) completedBooks.add(1);
    return ok;
}

export default function () {
    // VU 최초 실행 시 계정 + 캐릭터 생성
    if (token === null) {
        token = signup();
        if (!token) return;
        characterId = createCharacter(token);
        if (!characterId) return;
    }

    // 매 iteration: BIP 생성 → 완료 (핵심 측정 대상)
    const bipId = initBook(token, characterId);
    if (!bipId) return;

    completeBook(token, bipId);

    sleep(0.1); // 약간의 think time
}
