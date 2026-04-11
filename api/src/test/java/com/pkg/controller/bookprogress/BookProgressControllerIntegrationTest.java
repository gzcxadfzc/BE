package com.pkg.controller.bookprogress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pkg.TestApplication;
import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.AccessTokenAuthenticator;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.domain.book.BookPage;
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
    @DisplayName("GET /api/v1/book/progress/{id}/status - Lambda 완료 시 COMPLETED + 페이지 반환")
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
    @DisplayName("GET /api/v1/book/progress/{id}/status - 폴링 후 result 키 삭제 (멱등성)")
    void pollPageStatus_shouldDeleteResult_afterCompleted() throws Exception {
        String bipId = initBookAndGetBipId();
        publishLambdaResult(bipId, 0);

        // 첫 번째 폴링: COMPLETED
        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 두 번째 폴링: result 키 삭제됐으므로 PENDING
        mockMvc.perform(get("/api/v1/book/progress/{id}/status", bipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(print());
    }

    // ==================== 헬퍼 ====================

    /** Lambda가 페이지를 추가한 상황을 시뮬레이션 */
    private void addPageToBip(String bipId) {
        BookPage page = new BookPage("Once upon a time...", "https://example.com/page1.jpg", 0);
        bookInProgressRepository.addPageTo(bipId, page);
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
