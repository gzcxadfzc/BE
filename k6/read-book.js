import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://ec2-3-35-50-130.ap-northeast-2.compute.amazonaws.com:8080';

const boardAllDuration   = new Trend('board_all_duration',   true);
const bookDetailDuration = new Trend('book_detail_duration', true);
const myBooksDuration    = new Trend('my_books_duration',    true);
const myCharsDuration    = new Trend('my_chars_duration',    true);
const errorRate = new Rate('error_rate');

// book_setup: 25 VUs × 45s でデータ生成
// ramp_up   : 55s 후 읽기 부하 테스트
export const options = {
    scenarios: {
        book_setup: {
            executor: 'constant-vus',
            vus: 25,
            duration: '45s',
            gracefulStop: '5s',
            exec: 'createBooks',
        },
        ramp_up: {
            executor: 'ramping-vus',
            startTime: '55s',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10  },
                { duration: '1m',  target: 160 },
                { duration: '2m',  target: 160 },
                { duration: '30s', target: 0   },
            ],
            exec: 'readBooks',
        },
    },
    thresholds: {
        'board_all_duration':                ['p(95)<500'],
        'book_detail_duration':              ['p(95)<300'],
        'my_books_duration':                 ['p(95)<300'],
        'error_rate':                        ['rate<0.05'],
        'http_req_failed{scenario:ramp_up}': ['rate<0.05'],
    },
};

// ── 공통 폴링 헬퍼 ────────────────────────────────────────────

/**
 * 캐릭터 생성 완료 대기 (char:result:{cipId} 존재 여부)
 * @returns {boolean} READY 상태 도달 여부
 */
function pollCharacterReady(tok, cipId, maxAttempts = 30, intervalSec = 1) {
    for (let i = 0; i < maxAttempts; i++) {
        const res = http.get(
            `${BASE_URL}/api/v1/character/${cipId}/status`,
            { headers: { Authorization: `Bearer ${tok}` } }
        );
        if (res.status === 200 && res.json('data.status') === 'READY') return true;
        sleep(intervalSec);
    }
    return false;
}

/**
 * 페이지 생성 완료 대기 (bip:result:{bipId} 존재 여부)
 * @returns {boolean} COMPLETED 상태 도달 여부
 */
function pollPageCompleted(tok, bipId, maxAttempts = 30, intervalSec = 1) {
    for (let i = 0; i < maxAttempts; i++) {
        const res = http.get(
            `${BASE_URL}/api/v1/book/progress/${bipId}/status`,
            { headers: { Authorization: `Bearer ${tok}` } }
        );
        if (res.status === 200 && res.json('data.status') === 'COMPLETED') return true;
        sleep(intervalSec);
    }
    return false;
}

// ── book_setup VU 상태 ────────────────────────────────────────

let setupToken  = null;
let setupCharId = null;

export function createBooks() {
    // VU 최초 실행 시 유저 + 캐릭터 생성
    if (setupToken === null) {
        const username = `setup_vu${__VU}_${Date.now()}`;
        const signupRes = http.post(
            `${BASE_URL}/api/v1/auth/signup`,
            JSON.stringify({ username, password: 'loadtest123!' }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        if (signupRes.status !== 200) return;
        setupToken = signupRes.json('data.accessToken');

        // 1. 캐릭터 생성 요청 → 202 + cipId
        const charRes = http.post(
            `${BASE_URL}/api/v1/character/create`,
            JSON.stringify({
                name: `캐릭터_setup${__VU}`,
                personality: '밝은',
                userDescription: '설정용',
                appearanceDescription: '초록 눈',
            }),
            { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${setupToken}` } }
        );
        if (charRes.status !== 202) return;
        const cipId = charRes.json('data.cipId');

        // 2. Lambda 처리 완료 대기
        if (!pollCharacterReady(setupToken, cipId)) return;

        // 3. 캐릭터 완성 → DB 저장 → characterId 획득
        const completeCharRes = http.post(
            `${BASE_URL}/api/v1/character/${cipId}/complete`,
            null,
            { headers: { Authorization: `Bearer ${setupToken}` } }
        );
        if (completeCharRes.status !== 200) return;
        setupCharId = completeCharRes.json('data.id');
    }

    if (!setupCharId) return;

    // 매 iteration: 책 1권 생성 (init → 폴링 → complete)
    const initRes = http.post(
        `${BASE_URL}/api/v1/book/progress/init`,
        JSON.stringify({ characterId: setupCharId, backgroundInfo: '마법의 숲', userInput: '설정용 책' }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${setupToken}` } }
    );
    if (initRes.status !== 202) return;
    const bipId = initRes.json('data.bipId');

    if (!pollPageCompleted(setupToken, bipId)) return;

    http.post(
        `${BASE_URL}/api/v1/book/progress/${bipId}/complete`,
        JSON.stringify({ title: `설정용_${bipId}`, author: `setup${__VU}` }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${setupToken}` } }
    );
}

// ── ramp_up VU 상태 ───────────────────────────────────────────

let readToken   = null;
let readBookIds = null;

export function readBooks() {
    if (readToken === null) {
        const username = `read_vu${__VU}_${Date.now()}`;
        const signupRes = http.post(
            `${BASE_URL}/api/v1/auth/signup`,
            JSON.stringify({ username, password: 'loadtest123!' }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        if (signupRes.status !== 200) return;
        readToken = signupRes.json('data.accessToken');
    }

    if (readBookIds === null) {
        const boardRes = http.get(
            `${BASE_URL}/api/v1/book/board/all?index=0&size=100&sort=createdAtDesc`
        );
        readBookIds = boardRes.status === 200
            ? boardRes.json('data.elements').map(b => b.bookId)
            : [];
        console.log(`VU${__VU} bookId ${readBookIds.length}개 로드`);
    }

    const bookId = readBookIds.length > 0 ? readBookIds[__VU % readBookIds.length] : null;

    getBoardAll();
    if (bookId) getBookDetail(bookId);
    getMyBooks(readToken);
    getMyCharacters(readToken);

    sleep(0.1);
}

// ── 조회 함수들 ───────────────────────────────────────────────

function getBoardAll() {
    const start = Date.now();
    const res = http.get(
        `${BASE_URL}/api/v1/book/board/all?index=0&size=10&sort=createdAtDesc`,
        { tags: { name: 'board_all' } }
    );
    boardAllDuration.add(Date.now() - start);
    const ok = check(res, { 'board_all 200': r => r.status === 200 });
    errorRate.add(!ok);
}

function getBookDetail(bId) {
    const start = Date.now();
    const res = http.get(
        `${BASE_URL}/api/v1/book/board/${bId}`,
        { tags: { name: 'book_detail' } }
    );
    bookDetailDuration.add(Date.now() - start);
    const ok = check(res, { 'book_detail 200': r => r.status === 200 });
    errorRate.add(!ok);
}

function getMyBooks(tok) {
    const start = Date.now();
    const res = http.get(
        `${BASE_URL}/api/v1/book/my`,
        { headers: { Authorization: `Bearer ${tok}` }, tags: { name: 'my_books' } }
    );
    myBooksDuration.add(Date.now() - start);
    const ok = check(res, { 'my_books 200': r => r.status === 200 });
    errorRate.add(!ok);
}

function getMyCharacters(tok) {
    const start = Date.now();
    const res = http.get(
        `${BASE_URL}/api/v1/character/my`,
        { headers: { Authorization: `Bearer ${tok}` }, tags: { name: 'my_chars' } }
    );
    myCharsDuration.add(Date.now() - start);
    const ok = check(res, { 'my_chars 200': r => r.status === 200 });
    errorRate.add(!ok);
}

// ─────────────────────────────────────────────────────────────

export function handleSummary(data) {
    const now = new Date();
    const timestamp = now.toISOString().slice(0, 19).replace('T', '_').replace(/:/g, '');
    return {
        [`output_${timestamp}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
