import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://ec2-3-35-50-130.ap-northeast-2.compute.amazonaws.com:8080';

const boardAllDuration  = new Trend('board_all_duration',  true);
const bookDetailDuration = new Trend('book_detail_duration', true);
const myBooksDuration   = new Trend('my_books_duration',   true);
const myCharsDuration   = new Trend('my_chars_duration',   true);
const errorRate = new Rate('error_rate');

// book_setup: 25 VUs × 45s ≈ 10,000권 생성
// ramp_up   : 55s 후 시작, 읽기 부하 테스트
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
                { duration: '30s', target: 10  },  // 워밍업
                { duration: '1m',  target: 160 },  // 부하 증가
                { duration: '2m',  target: 160 },  // 안정 구간
                { duration: '30s', target: 0   },  // 쿨다운
            ],
            exec: 'readBooks',
        },
    },
    thresholds: {
        'board_all_duration':              ['p(95)<500'],
        'book_detail_duration':            ['p(95)<300'],
        'my_books_duration':               ['p(95)<300'],
        'error_rate':                      ['rate<0.05'],
        'http_req_failed{scenario:ramp_up}': ['rate<0.05'],
    },
};

// ── book_setup VU 상태 (VU당 독립) ───────────────────────

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
        if (charRes.status !== 200) return;
        setupCharId = charRes.json('data.id');
    }

    // 매 iteration: 책 1권 생성
    const initRes = http.post(
        `${BASE_URL}/api/v1/book/progress/init`,
        JSON.stringify({ characterId: setupCharId, backgroundInfo: '마법의 숲', userInput: '설정용 책' }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${setupToken}` } }
    );
    if (initRes.status !== 200) return;
    const bipId = initRes.json('data.bookInProgress.id');

    http.post(
        `${BASE_URL}/api/v1/book/progress/${bipId}/complete`,
        JSON.stringify({ title: `설정용_${bipId}`, author: `setup${__VU}` }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${setupToken}` } }
    );
}

// ── ramp_up VU 상태 (VU당 독립) ─────────────────────────

let readToken  = null;
let readBookIds = null;

export function readBooks() {
    // VU 최초 실행 시 유저 생성
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

    // bookId 목록 최초 1회 조회 (book_setup 완료 후 충분한 데이터 존재)
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

// ── 조회 함수들 ──────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────

export function handleSummary(data) {
    const now = new Date();
    const timestamp = now.toISOString().slice(0, 19).replace('T', '_').replace(/:/g, '');
    const filename = `output_${timestamp}.json`;
    return {
        [filename]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
