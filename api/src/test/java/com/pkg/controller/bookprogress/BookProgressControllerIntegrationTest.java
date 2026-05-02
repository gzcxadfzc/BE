package com.pkg.controller.bookprogress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pkg.TestApplication;
import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.AccessTokenAuthenticator;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.bookprogress.BookInProgressRepository;
import com.pkg.domain.bookprogress.BookPageQueuePublisher;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.member.Role;
import org.springframework.data.redis.core.RedisTemplate;
import com.pkg.jpa.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test-in-memory")
@DisplayName("BookProgressController 통합 테스트")
class BookProgressControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccessTokenAuthenticator mockTokenAuthenticator;

    @MockBean
    private BookPageQueuePublisher mockQueuePublisher;

    @MockBean
    private ImageRepository mockImageRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CharacterJpaRepository characterJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private BookJpaRepository bookJpaRepository;

    @Autowired
    private BookPageJpaRepository pageJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BookInProgressRepository bookInProgressRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Long testMemberId;
    private Long testCharacterId;

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @BeforeEach
    void setUp() {
        pageJpaRepository.deleteAll();
        bookJpaRepository.deleteAll();
        characterJpaRepository.deleteAll();
        memberJpaRepository.deleteAll();
        entityManager.clear();

        MemberJpaEntity member = MemberJpaEntity.builder()
                .username("testuser")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(member);
        testMemberId = member.getId();

        CharacterJpaEntity character = CharacterJpaEntity.builder()
                .memberId(testMemberId)
                .name("Alice")
                .appearanceKeywords("blonde, blue eyes")
                .personality("curious and brave")
                .userDescription("A young girl who loves adventure")
                .imageUrl("https://example.com/character.jpg")
                .originImageUrl("https://example.com/origin.jpg")
                .build();
        characterJpaRepository.save(character);
        testCharacterId = character.getId();

        doNothing().when(mockQueuePublisher).publish(any());

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(testMemberId, Role.MEMBER));
    }

    // ==================== initBook ====================

    @Test
    @DisplayName("POST /api/v1/book/progress/init - 202 반환 및 bipId 포함")
    void initBook_shouldReturn202WithBipId() throws Exception {
        BookInitRequest request = new BookInitRequest(
                "A magical forest full of mystery",
                testCharacterId,
                "I want Alice to discover a hidden treasure"
        );

        mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.bipId").isNotEmpty())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/init - 존재하지 않는 캐릭터로 초기화 시 404")
    void initBook_shouldReturn404_whenCharacterNotFound() throws Exception {
        BookInitRequest request = new BookInitRequest(
                "A magical forest",
                99999L,
                "User input"
        );

        mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    // ==================== generatePage ====================

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 202 반환 및 bipId 포함")
    void generatePage_shouldReturn202WithBipId() throws Exception {
        String bipId = initBookAndGetBipId();

        BookProgressRequest pageRequest = new BookProgressRequest("Alice decides to explore deeper");

        mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pageRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.bipId").value(bipId))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - guard 없는 stale PENDING은 자동 복구 후 202")
    void generatePage_shouldRecover_whenStalePending() throws Exception {
        String bipId = initBookAndGetBipId();

        // guard 없이 PENDING 상태 강제 설정 (Lambda 하드 크래시 시뮬레이션)
        forcePendingWithoutGuard(bipId);

        BookProgressRequest pageRequest = new BookProgressRequest("Continue story");

        mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pageRequest)))
                .andExpect(status().isAccepted())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - BIP가 이미 PENDING이면 409 반환")
    void generatePage_shouldReturn409_whenAlreadyPending() throws Exception {
        String bipId = initBookAndGetBipId();

        BookProgressRequest pageRequest = new BookProgressRequest("Continue story");
        String requestBody = objectMapper.writeValueAsString(pageRequest);

        // 첫 번째 요청: IN_PROGRESS → PENDING, 202 반환
        mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted());

        // 두 번째 요청: PENDING 상태이므로 409 반환
        mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 동시에 같은 책에 요청 시 하나는 409 반환")
    void generatePage_shouldReturn409_whenConcurrentRequestsToSameBook() throws Exception {
        String bipId = initBookAndGetBipId();

        BookProgressRequest pageRequest = new BookProgressRequest("Continue story");
        String requestBody = objectMapper.writeValueAsString(pageRequest);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn().getResponse().getStatus();
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();
                Thread.sleep(50);
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn().getResponse().getStatus();
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        thread1.start();
        thread2.start();
        startLatch.countDown();

        assertThat(completeLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(statusCodes).hasSize(2);
        assertThat(statusCodes).containsExactlyInAnyOrder(202, 409);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 서로 다른 책에 동시 요청 시 모두 202")
    void generatePage_shouldSucceed_whenConcurrentRequestsToDifferentBooks() throws Exception {
        String bipId1 = initBookAndGetBipId();
        String bipId2 = initBookAndGetBipId();

        BookProgressRequest pageRequest = new BookProgressRequest("Continue");
        String requestBody = objectMapper.writeValueAsString(pageRequest);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId1)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn().getResponse().getStatus();
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId2)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn().getResponse().getStatus();
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        thread1.start();
        thread2.start();
        startLatch.countDown();

        assertThat(completeLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(statusCodes).hasSize(2);
        assertThat(statusCodes).containsOnly(202);
    }

    // ==================== retrieveBookInProgress ====================

    @Test
    @DisplayName("GET /api/v1/book/progress/{id} - 진행중인 책 조회 성공")
    void retrieveBookInProgress_shouldReturnDetails() throws Exception {
        String bipId = initBookAndGetBipId();

        mockMvc.perform(get("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(bipId))
                .andExpect(jsonPath("$.data.mainCharacter.name").value("Alice"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/progress/{id} - 존재하지 않는 책 조회 시 404")
    void retrieveBookInProgress_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/book/progress/{id}", "non-existent-id")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/progress/{id} - 다른 사용자의 책 조회 시 403")
    void retrieveBookInProgress_shouldReturn403_whenAccessingOthersBook() throws Exception {
        String bipId = initBookAndGetBipId();

        MemberJpaEntity otherMember = MemberJpaEntity.builder()
                .username("otheruser")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(otherMember);

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(otherMember.getId(), Role.MEMBER));

        mockMvc.perform(get("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer other.token"))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    // ==================== completeBook ====================

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/complete - 책 완료 성공")
    void completeBook_shouldCreateBook() throws Exception {
        String bipId = initBookAndGetBipId();
        addPageToBip(bipId); // Lambda 역할 시뮬레이션: 페이지 추가 후 IN_PROGRESS 상태

        BookCompleteRequest completeRequest = new BookCompleteRequest("Alice's Adventure", "Test Author");

        mockMvc.perform(post("/api/v1/book/progress/{id}/complete", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("Alice's Adventure"))
                .andExpect(jsonPath("$.data.author").value("Test Author"))
                .andExpect(jsonPath("$.data.memberId").value(testMemberId))
                .andExpect(jsonPath("$.data.character.name").value("Alice"))
                .andDo(print());
    }

    // ==================== pollPageStatus ====================

    @Test
    @DisplayName("GET /api/v1/book/progress/{id}/status - Lambda 미완료 시 PENDING 반환")
    void pollPageStatus_shouldReturnPending_whenNoResult() throws Exception {
        String bipId = initBookAndGetBipId();

        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.page").doesNotExist())
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/progress/{id}/status - Lambda 완료 시 COMPLETED + 페이지 데이터 반환")
    void pollPageStatus_shouldReturnCompleted_whenResultExists() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.page.pageIndex").value(0))
                .andExpect(jsonPath("$.data.page.context").value("Alice found a magical door"))
                .andExpect(jsonPath("$.data.page.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.page.questions").isArray())
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/progress/{id}/status - confirm 전 반복 폴링은 계속 COMPLETED 반환 (멱등)")
    void pollPageStatus_shouldBeIdempotent_beforeConfirm() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        // 첫 번째 폴링: COMPLETED
        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 두 번째 폴링: result가 아직 남아있으므로 여전히 COMPLETED
        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/progress/{id}/status - BIP에 페이지가 추가되지 않음 (confirm 전)")
    void pollPageStatus_shouldNotUpdateBip() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        BookInProgress bip = bookInProgressRepository.retrieveById(bipId);
        assertThat(bip.previousPages()).isEmpty();
    }

    // ==================== confirmPage ====================

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/confirm - 204 반환 및 BIP 페이지 추가")
    void confirmPage_shouldReturn204AndUpdateBip() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNoContent())
                .andDo(print());

        BookInProgress bip = bookInProgressRepository.retrieveById(bipId);
        assertThat(bip.previousPages()).hasSize(1);
        assertThat(bip.previousPages().get(0).context()).isEqualTo("Alice found a magical door");
        assertThat(bip.status()).isEqualTo(BookInProgress.Status.IN_PROGRESS);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/confirm - confirm 후 result 키 삭제됨")
    void confirmPage_shouldDeleteResult() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNoContent());

        // confirm 후 poll → result 없으므로 PENDING
        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/confirm - confirm 후 PendingGuard 삭제됨")
    void confirmPage_shouldDeletePendingGuard() throws Exception {
        String bipId = initBookAndGetBipId();
        forceSetPendingGuard(bipId);
        publishLambdaResult(bipId, 0);

        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNoContent());

        // guard 삭제됐으므로 다음 generateWithAi 호출 가능
        Boolean guardExists = redisTemplate.hasKey("bip:pending-guard:" + bipId);
        assertThat(guardExists).isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/confirm - result 없을 때 no-op (이미 confirm된 경우)")
    void confirmPage_shouldBeNoOp_whenResultAlreadyDeleted() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        // 첫 번째 confirm
        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNoContent());

        // 두 번째 confirm: result 없으므로 no-op, 204 반환
        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNoContent())
                .andDo(print());

        // BIP 페이지는 1개 (중복 추가 없음)
        BookInProgress bip = bookInProgressRepository.retrieveById(bipId);
        assertThat(bip.previousPages()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/confirm - 동시 confirm 시 페이지 중복 추가 없음")
    void confirmPage_shouldNotDuplicatePage_whenConcurrentConfirm() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);

        Runnable confirmTask = () -> {
            try {
                startLatch.await();
                mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                        .andReturn();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        };

        new Thread(confirmTask).start();
        new Thread(confirmTask).start();
        startLatch.countDown();

        assertThat(completeLatch.await(10, TimeUnit.SECONDS)).isTrue();

        BookInProgress bip = bookInProgressRepository.retrieveById(bipId);
        assertThat(bip.previousPages()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/confirm - BIP 저장 후 result 삭제 실패 시나리오: 재시도해도 페이지 중복 없음")
    void confirmPage_shouldNotDuplicatePage_whenResultDeleteFailedOnFirstAttempt() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        // 첫 번째 confirm (정상 완료)
        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNoContent());

        // result가 삭제 실패했다고 가정: 동일한 result를 다시 저장
        publishLambdaResult(bipId, 0);

        // 재시도 confirm: BIP에 이미 pageIndex=0이 있으므로 저장 스킵
        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNoContent());

        BookInProgress bip = bookInProgressRepository.retrieveById(bipId);
        assertThat(bip.previousPages()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/confirm - 다른 사용자 confirm 시 403")
    void confirmPage_shouldReturn403_whenNotOwner() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        MemberJpaEntity otherMember = MemberJpaEntity.builder()
                .username("otheruser2")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(otherMember);

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(otherMember.getId(), Role.MEMBER));

        mockMvc.perform(post("/api/v1/book/progress/{id}/confirm", bipId)
                        .header("Authorization", "Bearer other.token"))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    // ==================== 헬퍼 ====================

    /** Lambda가 페이지를 추가한 상황을 시뮬레이션 */
    private void addPageToBip(String bipId) {
        BookPage page = new BookPage("Once upon a time...", "https://example.com/page1.jpg", 0);
        bookInProgressRepository.addPageTo(bipId, page);
    }

    /** Lambda 하드 크래시 시뮬레이션: guard 없이 PENDING 상태만 강제 설정 */
    private void forcePendingWithoutGuard(String bipId) {
        BookInProgress bip = bookInProgressRepository.retrieveById(bipId);
        bookInProgressRepository.save(bip.markAsPending());
        // guard는 설정하지 않음 → stale PENDING
    }

    /** PendingGuard 직접 설정 (confirm 후 삭제 여부 검증용) */
    private void forceSetPendingGuard(String bipId) {
        redisTemplate.opsForValue().set("bip:pending-guard:" + bipId, "1");
    }

    /** Lambda가 bip:result:{bipId}에 결과를 저장한 상황을 시뮬레이션 */
    private void publishLambdaResult(String bipId, int pageIndex) {
        String json = """
                {
                  "pageIndex": %d,
                  "context": "Alice found a magical door",
                  "imageUrl": "https://s3.example.com/bip/%s/page-%d.png",
                  "questions": ["What did Alice see?", "Where did she go?"]
                }
                """.formatted(pageIndex, bipId, pageIndex);
        redisTemplate.opsForValue().set("bip:result:" + bipId, json);
    }

    private String initBookAndGetBipId() throws Exception {
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest full of mystery",
                testCharacterId,
                "Start the adventure"
        );

        String response = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("bipId").asText();
    }
}
