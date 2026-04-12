import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://ec2-3-35-50-130.ap-northeast-2.compute.amazonaws.com:8080';

// Custom metrics
const initToCompletedDuration = new Trend('init_to_completed_duration', true); // initBook 202 → 폴링 COMPLETED
const completeBookDuration    = new Trend('complete_book_duration', true);      // completeBook POST 단독
const errorRate     = new Rate('error_rate');
const completedBooks = new Counter('completed_books');

export const options = {
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10  },
                { duration: '1m',  target: 100 },
                { duration: '2m',  target: 100 },
                { duration: '30s', target: 0   },
            ],
        },
    },
    thresholds: {
        'init_to_completed_duration': ['p(95)<15000'],  // Lambda 처리 포함 15s 이내
        'complete_book_duration':     ['p(95)<2000'],
        'error_rate':                 ['rate<0.05'],
        'http_req_failed':            ['rate<0.05'],
    },
};

// VU별 상태
let token       = null;
let characterId = null;

// ── 헬퍼 ─────────────────────────────────────────────────────

function signup() {
    const username = `load_vu${__VU}_${Date.now()}`;
    const res = http.post(
        `${BASE_URL}/api/v1/auth/signup`,
        JSON.stringify({ username, password: 'loadtest123!' }),
        { headers: { 'Content-Type': 'application/json' }, tags: { name: 'signup' } }
    );
    check(res, { 'signup 200': r => r.status === 200 });
    return res.json('data.accessToken');
}

/**
 * 캐릭터 생성 (비동기): POST → 폴링 → complete → characterId 반환
 */
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
    if (!check(res, { 'createCharacter 202': r => r.status === 202 })) return null;
    const cipId = res.json('data.cipId');

    // Lambda 처리 완료 대기
    for (let i = 0; i < 30; i++) {
        const statusRes = http.get(
            `${BASE_URL}/api/v1/character/${cipId}/status`,
            { headers: { Authorization: `Bearer ${tok}` } }
        );
        if (statusRes.status === 200 && statusRes.json('data.status') === 'READY') break;
        sleep(1);
        if (i === 29) return null; // 타임아웃
    }

    // 캐릭터 완성
    const completeRes = http.post(
        `${BASE_URL}/api/v1/character/${cipId}/complete`,
        null,
        { headers: { Authorization: `Bearer ${tok}` }, tags: { name: 'completeCharacter' } }
    );
    if (!check(completeRes, { 'completeCharacter 200': r => r.status === 200 })) return null;
    return completeRes.json('data.id');
}

/**
 * 책 초기화 → Lambda 처리 완료 대기 (전체 시간 측정)
 * @returns {string|null} bipId
 */
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
    if (!check(res, { 'initBook 202': r => r.status === 202 })) {
        errorRate.add(1);
        return null;
    }
    const bipId = res.json('data.bipId');

    // Lambda 처리 완료 폴링
    let completed = false;
    for (let i = 0; i < 30; i++) {
        const statusRes = http.get(
            `${BASE_URL}/api/v1/book/progress/${bipId}/status`,
            { headers: { Authorization: `Bearer ${tok}` } }
        );
        if (statusRes.status === 200 && statusRes.json('data.status') === 'COMPLETED') {
            completed = true;
            break;
        }
        sleep(1);
    }

    initToCompletedDuration.add(Date.now() - start);
    if (!completed) {
        errorRate.add(1);
        return null;
    }
    return bipId;
}

function completeBook(tok, bipId) {
    const start = Date.now();
    const res = http.post(
        `${BASE_URL}/api/v1/book/progress/${bipId}/complete`,
        JSON.stringify({ title: `부하테스트 동화_${bipId}`, author: `VU${__VU}` }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` }, tags: { name: 'completeBook' } }
    );
    completeBookDuration.add(Date.now() - start);
    const ok = check(res, { 'completeBook 200': r => r.status === 200 });
    errorRate.add(!ok);
    if (ok) completedBooks.add(1);
    return ok;
}

// ── 메인 시나리오 ─────────────────────────────────────────────

export default function () {
    // VU 최초 실행 시 계정 + 캐릭터 생성
    if (token === null) {
        token = signup();
        if (!token) return;
        characterId = createCharacter(token);
        if (!characterId) return;
    }

    // 매 iteration: BIP 생성(비동기) → 폴링 → 완료
    const bipId = initBook(token, characterId);
    if (!bipId) return;

    completeBook(token, bipId);

    sleep(0.1);
}
