package com.pkg.controller.character;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pkg.TestApplication;
import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.AccessTokenAuthenticator;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.domain.ai.BookCharacterGenerator;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
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

    // OpenAI API를 사용하는 BookCharacterGenerator를 Mock으로 주입
    @MockBean
    private BookCharacterGenerator mockBookCharacterGenerator;

    // S3 등 외부 서비스를 사용하는 ImageRepository를 Mock으로 주입
    @MockBean
    private ImageRepository mockImageRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CharacterJpaRepository characterJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private EntityManager entityManager;

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
        // Clean up database
        characterJpaRepository.deleteAll();
        memberJpaRepository.deleteAll();
        entityManager.clear();

        // Insert test member
        MemberJpaEntity member = MemberJpaEntity.builder()
                .username("testuser")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(member);
        testMemberId = member.getId();

        // Insert test characters
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

        // Mock 설정 - BookCharacterGenerator (OpenAI API 대신 stub 응답 반환)
        when(mockBookCharacterGenerator.generateImageFrom(any()))
                .thenReturn("https://openai-generated-character-image.com/character.png");

        // Mock 설정 - ImageRepository (S3 업로드 대신 stub 응답 반환)
        when(mockImageRepository.uploadCharacterImage(any()))
                .thenReturn(new ImageUploadResult(
                        "https://s3.amazonaws.com/characters/uploaded-character.jpg",
                        "uploaded-character.jpg"
                ));

        // Mock 설정 - AccessTokenAuthenticator
        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(testMemberId, Role.MEMBER));
    }

    @Test
    @DisplayName("GET /api/v1/character/my - 인증된 사용자의 캐릭터 목록을 조회할 수 있다")
    void retrieveMy_shouldReturnCharacterList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/character/my")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(testCharacter1Id))
                .andExpect(jsonPath("$.data[0].name").value("Alice"))
                .andExpect(jsonPath("$.data[0].personality").value("curious and brave"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/alice.jpg"))
                .andExpect(jsonPath("$.data[0].description").value("A young girl who loves adventure"))
                .andExpect(jsonPath("$.data[1].id").value(testCharacter2Id))
                .andExpect(jsonPath("$.data[1].name").value("Bob"))
                .andExpect(jsonPath("$.data[1].personality").value("shy and thoughtful"))
                .andExpect(jsonPath("$.data[1].imageUrl").value("https://example.com/bob.jpg"))
                .andExpect(jsonPath("$.data[1].description").value("A quiet observer"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/character/my - 캐릭터가 없는 사용자는 빈 배열을 받는다")
    void retrieveMy_shouldReturnEmptyList_whenUserHasNoCharacters() throws Exception {
        // Given - 새로운 사용자 생성 (캐릭터 없음)
        MemberJpaEntity newMember = MemberJpaEntity.builder()
                .username("newuser")
                .password("newpass123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        MemberJpaEntity savedMember = memberJpaRepository.save(newMember);

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(savedMember.getId(), Role.MEMBER));

        // When & Then
        mockMvc.perform(get("/api/v1/character/my")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/character/my - 다른 사용자의 캐릭터는 조회되지 않는다")
    void retrieveMy_shouldNotReturnOtherUsersCharacters() throws Exception {
        // Given - 다른 사용자와 그 사용자의 캐릭터 생성
        MemberJpaEntity otherMember = MemberJpaEntity.builder()
                .username("otheruser")
                .password("otherpass123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(otherMember);

        CharacterJpaEntity otherCharacter = CharacterJpaEntity.builder()
                .memberId(otherMember.getId())
                .name("Charlie")
                .appearanceKeywords("red hair")
                .personality("adventurous")
                .userDescription("A brave warrior")
                .imageUrl("https://example.com/charlie.jpg")
                .originImageUrl("https://openai.com/origin-charlie.jpg")
                .build();
        characterJpaRepository.save(otherCharacter);

        // When & Then - testMemberId로 조회하면 otherMember의 캐릭터는 안 나와야 함
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
    @DisplayName("GET /api/v1/character/{id} - ID로 캐릭터를 조회할 수 있다")
    void retrieveById_shouldReturnCharacter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/character/board/{id}", testCharacter1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(testCharacter1Id))
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.personality").value("curious and brave"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/alice.jpg"))
                .andExpect(jsonPath("$.data.description").value("A young girl who loves adventure"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/character/{id} - 존재하지 않는 캐릭터 조회 시 예외가 발생한다")
    void retrieveById_shouldThrowException_whenCharacterNotFound() throws Exception {
        // Given
        Long nonExistentId = 99999L;

        // When & Then
        mockMvc.perform(get("/api/v1/character/board/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/character/create - 필수 필드 누락 시 적절한 검증 오류가 발생한다")
    void createBookCharacter_shouldValidateRequiredFields() throws Exception {
        // Given - 이름이 누락된 요청
        String invalidRequest = """
                {
                    "personality": "brave",
                    "userDescription": "test",
                    "appearanceDescription": "test"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/character/create")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().is4xxClientError())
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/character/board/{id} - 다른 사용자의 캐릭터도 ID로 조회할 수 있다 (공개된 정보)")
    void retrieveById_shouldAllowAccessToOtherUsersCharacters() throws Exception {
        // Given - 다른 사용자의 캐릭터
        MemberJpaEntity otherMember = MemberJpaEntity.builder()
                .username("otheruser")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(otherMember);

        CharacterJpaEntity otherCharacter = CharacterJpaEntity.builder()
                .memberId(otherMember.getId())
                .name("PublicCharacter")
                .appearanceKeywords("mysterious")
                .personality("enigmatic")
                .userDescription("A mysterious figure")
                .imageUrl("https://example.com/public.jpg")
                .originImageUrl("https://openai.com/origin-public.jpg")
                .build();
        CharacterJpaEntity saved = characterJpaRepository.save(otherCharacter);

        // When & Then - testMemberId 사용자가 다른 사용자의 캐릭터 조회
        mockMvc.perform(get("/api/v1/character/board/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("PublicCharacter"))
                .andDo(print());
    }
}
