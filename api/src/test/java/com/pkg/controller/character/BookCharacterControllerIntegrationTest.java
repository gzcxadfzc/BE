package com.pkg.controller.character;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pkg.TestApplication;
import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.AccessTokenAuthenticator;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.domain.character.CharacterQueuePublisher;
import com.pkg.domain.member.Role;
import com.pkg.jpa.CharacterJpaEntity;
import com.pkg.jpa.CharacterJpaRepository;
import com.pkg.jpa.MemberJpaEntity;
import com.pkg.jpa.MemberJpaRepository;
import com.pkg.jpa.RoleJpa;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test-in-memory")
@DisplayName("BookCharacterController 통합 테스트")
class BookCharacterControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccessTokenAuthenticator mockTokenAuthenticator;

    @MockBean
    private CharacterQueuePublisher mockQueuePublisher;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CharacterJpaRepository characterJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Long testMemberId;
    private Long testCharacter1Id;
    private Long testCharacter2Id;

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @BeforeEach
    void setUp() {
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

        CharacterJpaEntity character1 = CharacterJpaEntity.builder()
                .memberId(testMemberId)
                .name("Alice")
                .appearanceKeywords("blonde, blue eyes")
                .personality("curious and brave")
                .userDescription("A young girl who loves adventure")
                .imageUrl("https://example.com/alice.jpg")
                .originImageUrl("https://openai.com/origin-alice.jpg")
                .build();
        characterJpaRepository.save(character1);
        testCharacter1Id = character1.getId();

        CharacterJpaEntity character2 = CharacterJpaEntity.builder()
                .memberId(testMemberId)
                .name("Bob")
                .appearanceKeywords("tall, dark hair")
                .personality("shy and thoughtful")
                .userDescription("A quiet observer")
                .imageUrl("https://example.com/bob.jpg")
                .originImageUrl("https://openai.com/origin-bob.jpg")
                .build();
        characterJpaRepository.save(character2);
        testCharacter2Id = character2.getId();

        doNothing().when(mockQueuePublisher).publish(any());

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(testMemberId, Role.MEMBER));
    }

    // ── 조회 테스트 ───────────────────────────────────────────────────────��──

    @Test
    @DisplayName("GET /api/v1/character/my - 인증된 사용자의 캐릭터 목록을 조회할 수 있다")
    void retrieveMy_shouldReturnCharacterList() throws Exception {
        mockMvc.perform(get("/api/v1/character/my")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Alice"))
                .andExpect(jsonPath("$.data[1].name").value("Bob"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/character/my - 캐릭터가 없는 사용자는 빈 배열을 받는다")
    void retrieveMy_shouldReturnEmptyList_whenUserHasNoCharacters() throws Exception {
        MemberJpaEntity newMember = MemberJpaEntity.builder()
                .username("newuser").password("newpass123")
                .role(RoleJpa.MEMBER).authProvider("local").build();
        memberJpaRepository.save(newMember);
        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(newMember.getId(), Role.MEMBER));

        mockMvc.perform(get("/api/v1/character/my")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/character/board/{id} - ID로 캐릭터를 조회할 수 있다")
    void retrieveById_shouldReturnCharacter() throws Exception {
        mockMvc.perform(get("/api/v1/character/board/{id}", testCharacter1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/character/board/{id} - 존재하지 않는 캐릭터 조회 시 예외가 발생한다")
    void retrieveById_shouldThrowException_whenCharacterNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/character/board/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    // ── 비동기 생성 흐름 테스트 ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /create - 202 반환 + cipId 포함")
    void requestCreate_shouldReturn202WithCipId() throws Exception {
        String body = """
                {
                    "name": "TestChar",
                    "personality": "brave",
                    "userDescription": "A hero",
                    "appearanceDescription": "tall and strong"
                }
                """;

        mockMvc.perform(post("/api/v1/character/create")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.cipId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /{cipId}/status - Lambda 결과 없으면 PENDING")
    void pollStatus_shouldReturnPending_whenResultNotReady() throws Exception {
        String cipId = createCip();

        mockMvc.perform(get("/api/v1/character/{cipId}/status", cipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(print());

        cleanupCip(cipId);
    }

    @Test
    @DisplayName("GET /{cipId}/status - Lambda 결과 있으면 READY")
    void pollStatus_shouldReturnReady_whenResultExists() throws Exception {
        String cipId = createCip();
        publishLambdaResult(cipId, "https://mock-s3/char.png");

        mockMvc.perform(get("/api/v1/character/{cipId}/status", cipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andDo(print());

        cleanupCip(cipId);
        redisTemplate.delete("char:result:" + cipId);
    }

    @Test
    @DisplayName("POST /{cipId}/complete - 결과 있으면 DB 저장 + 200 반환")
    void completeCharacter_shouldSaveToDb_whenResultExists() throws Exception {
        String cipId = createCip();
        publishLambdaResult(cipId, "https://mock-s3/char.png");

        mockMvc.perform(post("/api/v1/character/{cipId}/complete", cipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("TestChar"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://mock-s3/char.png"))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /{cipId}/complete - 결과 없으면 409")
    void completeCharacter_shouldReturn409_whenResultNotReady() throws Exception {
        String cipId = createCip();

        mockMvc.perform(post("/api/v1/character/{cipId}/complete", cipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isConflict())
                .andDo(print());

        cleanupCip(cipId);
    }

    @Test
    @DisplayName("POST /{cipId}/complete - 이미 완료된 CIP에 재호출 시 409")
    void completeCharacter_shouldReturn409_whenAlreadyCompleted() throws Exception {
        String cipId = createCip();
        publishLambdaResult(cipId, "https://mock-s3/char.png");

        // 첫 번째 complete - 성공
        mockMvc.perform(post("/api/v1/character/{cipId}/complete", cipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk());

        // 두 번째 complete - COMPLETED 상태이므로 409
        mockMvc.perform(post("/api/v1/character/{cipId}/complete", cipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private String createCip() throws Exception {
        String body = """
                {
                    "name": "TestChar",
                    "personality": "brave",
                    "userDescription": "A hero",
                    "appearanceDescription": "tall and strong"
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/character/create")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("data").get("cipId").asText();
    }

    private void publishLambdaResult(String cipId, String imageUrl) {
        String json = "{\"imageUrl\":\"" + imageUrl + "\"}";
        redisTemplate.opsForValue().set("char:result:" + cipId, json);
    }

    private void cleanupCip(String cipId) {
        redisTemplate.delete("char:progress:" + cipId);
    }
}
