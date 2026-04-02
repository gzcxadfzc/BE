package com.pkg.controller.bookprogress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pkg.TestApplication;
import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.AccessTokenAuthenticator;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.domain.ai.BookPageGenerated;
import com.pkg.domain.ai.BookPageGenerator;
import com.pkg.domain.book.Book;
import com.pkg.domain.bookprogress.BookCompleteExecutor;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.bookprogress.BookInProgressRepository;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import com.pkg.domain.member.Role;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
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
    private BookPageGenerator mockBookPageGenerator;

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

    private Long testMemberId;
    private Long testCharacterId;

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @BeforeEach
    void setUp() {
        // Clean up database and redis
        pageJpaRepository.deleteAll();
        bookJpaRepository.deleteAll();
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

        // Insert test character
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

        // Mock 설정 - BookPageGenerator (OpenAI API 대신 stub 응답 반환)
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenReturn(new BookPageGenerated(
                        "https://openai-generated-image-url.com/image.png",
                        "Once upon a time, there was a brave girl named Alice...",
                        List.of("What happens next?", "Where does Alice go?")
                ));

        // Mock 설정 - ImageRepository (S3 업로드 대신 stub 응답 반환)
        when(mockImageRepository.uploadTemporary(any()))
                .thenReturn(new ImageUploadResult(
                        "https://s3.amazonaws.com/temp/uploaded-image.jpg",
                        "uploaded-image.jpg"
                ));

        // Mock 설정 - AccessTokenAuthenticator
        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(testMemberId, Role.MEMBER));
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/init - 책 생성 초기화 성공")
    void initBook_shouldReturnBookGenerationResponse() throws Exception {
        // Given
        BookInitRequest request = new BookInitRequest(
                "A magical forest full of mystery",
                testCharacterId,
                "I want Alice to discover a hidden treasure"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.bookInProgress.id").exists())
                .andExpect(jsonPath("$.data.bookInProgress.generatedPages", hasSize(1)))
                .andExpect(jsonPath("$.data.bookInProgress.status").value("IN_PROGRESS"))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/init - 존재하지 않는 캐릭터로 초기화 시도 시 예외 발생")
    void initBook_shouldThrowException_whenCharacterNotFound() throws Exception {
        // Given
        BookInitRequest request = new BookInitRequest(
                "A magical forest",
                99999L, // 존재하지 않는 캐릭터 ID
                "User input"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 페이지 생성 성공")
    void generatePage_shouldAddNewPageToBookInProgress() throws Exception {
        // Given - 먼저 책 초기화
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest full of mystery",
                testCharacterId,
                "Start the adventure"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // Mock 설정 - 두 번째 페이지 생성
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenReturn(new BookPageGenerated(
                        "Alice found a mysterious door behind the old tree...",
                        "https://openai-generated-image-url.com/image2.png",
                        List.of("Should Alice open the door?")
                ));

        BookInitRequest pageRequest = new BookInitRequest(
                null,
                null,
                "Alice decides to explore deeper into the forest"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.bookInProgress.status").value("IN_PROGRESS"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/progress/{id} - 진행중인 책 조회 성공")
    void retrieveBookInProgress_shouldReturnBookInProgressDetails() throws Exception {
        // Given - 책 초기화
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest full of mystery",
                testCharacterId,
                "Start the adventure"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // When & Then
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
    @DisplayName("GET /api/v1/book/progress/{id} - 존재하지 않는 책 조회 시 예외 발생")
    void retrieveBookInProgress_shouldThrowException_whenBookNotFound() throws Exception {
        // Given
        String nonExistentBipId = "non-existent-bip-id";

        // When & Then
        mockMvc.perform(get("/api/v1/book/progress/{id}", nonExistentBipId)
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/progress/{id} - 다른 사용자의 책 조회 시 권한 예외 발생")
    void retrieveBookInProgress_shouldThrowException_whenAccessingOthersBook() throws Exception {
        // Given - 첫 번째 사용자로 책 생성
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest",
                testCharacterId,
                "Start"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // 두 번째 사용자 생성 및 인증 설정
        MemberJpaEntity otherMember = MemberJpaEntity.builder()
                .username("otheruser")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(otherMember);

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(otherMember.getId(), Role.MEMBER));

        // When & Then - 다른 사용자가 첫 번째 사용자의 책 조회 시도
        mockMvc.perform(get("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer other.token"))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/complete - 책 완료 성공")
    void completeBook_shouldCreateBookAndReturnBookResponse() throws Exception {
        // Given - 책 초기화 및 페이지 추가
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest full of mystery",
                testCharacterId,
                "Start the adventure"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        BookCompleteRequest completeRequest = new BookCompleteRequest(
                "Alice's Adventure",
                "Test Author"
        );

        when(mockImageRepository.copyAllToPermanentStorage(any()))
                .thenReturn(Map.of(
                        "uploaded-image.jpg",
                            new ImageUploadResult("https://s3.amazonaws.com/temp/uploaded-image.jpg", "https://s3.amazonaws.com/uploaded-image.jpg")
                        ));

        // When & Then
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
                .andExpect(jsonPath("$.data.bookPages", hasSize(1)))
                .andExpect(jsonPath("$.data.character.id").value(testCharacterId))
                .andExpect(jsonPath("$.data.character.name").value("Alice"))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/init - 여러 사용자가 동시에 책을 생성할 수 있다")
    void initBook_shouldSupportMultipleUsersCreatingBooksSimultaneously() throws Exception {
        // Given - 두 번째 사용자 및 캐릭터 생성
        MemberJpaEntity member2 = MemberJpaEntity.builder()
                .username("user2")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(member2);

        CharacterJpaEntity character2 = CharacterJpaEntity.builder()
                .memberId(member2.getId())
                .name("Bob")
                .appearanceKeywords("tall, strong")
                .personality("brave and kind")
                .userDescription("A brave warrior")
                .imageUrl("https://example.com/bob.jpg")
                .originImageUrl("https://example.com/bob-origin.jpg")
                .build();
        characterJpaRepository.save(character2);

        BookInitRequest request1 = new BookInitRequest(
                "Forest adventure",
                testCharacterId,
                "Alice's journey"
        );

        BookInitRequest request2 = new BookInitRequest(
                "Mountain quest",
                character2.getId(),
                "Bob's journey"
        );

        // When - 첫 번째 사용자 요청
        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(testMemberId, Role.MEMBER));

        String response1 = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer token1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 두 번째 사용자 요청
        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(member2.getId(), Role.MEMBER));

        String response2 = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer token2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then - 두 책이 서로 다른 ID로 생성되었는지 확인
        String bipId1 = objectMapper.readTree(response1)
                .path("data").path("bookInProgress").path("id").asText();
        String bipId2 = objectMapper.readTree(response2)
                .path("data").path("bookInProgress").path("id").asText();

        assert !bipId1.equals(bipId2);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 동일한 책에 여러 페이지를 순차적으로 추가할 수 있다")
    void generatePage_shouldAddMultiplePagesSequentially() throws Exception {
        // Given - 책 초기화
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest",
                testCharacterId,
                "Start"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // When - 페이지 2 추가
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenReturn(new BookPageGenerated(
                        "Page 2 content",
                        "https://image2.png",
                        List.of("Question 2")
                ));

        BookInitRequest page2Request = new BookInitRequest(null, null, "Continue story");
        mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(page2Request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookInProgress.generatedPages", hasSize(2)))
                .andDo(print());

        // 페이지 3 추가
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenReturn(new BookPageGenerated(
                        "Page 3 content",
                        "https://image3.png",
                        List.of("Question 3")
                ));

        BookInitRequest page3Request = new BookInitRequest(null, null, "More story");
        mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(page3Request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookInProgress.generatedPages", hasSize(3)))
                .andExpect(jsonPath("$.data.bookInProgress.generatedPages[0].index").value(0))
                .andExpect(jsonPath("$.data.bookInProgress.generatedPages[1].index").value(1))
                .andExpect(jsonPath("$.data.bookInProgress.generatedPages[2].index").value(2))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 동시에 같은 책에 페이지 생성 요청 시 락으로 인해 하나는 409 Conflict 응답")
    void generatePage_shouldReturn409Conflict_whenConcurrentRequestsToSameBook() throws Exception {
        // Given - 책 초기화
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest",
                testCharacterId,
                "Start"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // Mock 설정 - AI 생성에 시간이 걸리도록 시뮬레이션 (2초 지연)
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(2000); // 2초 지연으로 락이 유지되는 시간 확보
                    return new BookPageGenerated(
                            "Generated page content",
                            "https://image.png",
                            List.of("Question")
                    );
                });

        BookInitRequest pageRequest = new BookInitRequest(null, null, "Continue story");
        String requestBody = objectMapper.writeValueAsString(pageRequest);

        // When - 동시에 두 개의 요청을 같은 책에 보냄
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);

        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        // 첫 번째 스레드
        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        // 두 번째 스레드
        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();
                Thread.sleep(100); // 첫 번째 요청이 락을 먼저 획득하도록 약간의 지연
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        thread1.start();
        thread2.start();

        startLatch.countDown(); // 두 스레드 동시 시작

        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(statusCodes).hasSize(2);

        // 하나는 성공(200), 하나는 충돌(409)이어야 함
        assertThat(statusCodes).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 서로 다른 책에 대한 동시 요청은 모두 성공")
    void generatePage_shouldSucceed_whenConcurrentRequestsToDifferentBooks() throws Exception {
        // Given - 두 개의 책 초기화
        BookInitRequest initRequest1 = new BookInitRequest(
                "Forest adventure",
                testCharacterId,
                "Start 1"
        );

        String initResponse1 = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest1)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId1 = objectMapper.readTree(initResponse1)
                .path("data").path("bookInProgress").path("id").asText();

        BookInitRequest initRequest2 = new BookInitRequest(
                "Mountain quest",
                testCharacterId,
                "Start 2"
        );

        String initResponse2 = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId2 = objectMapper.readTree(initResponse2)
                .path("data").path("bookInProgress").path("id").asText();

        // Mock 설정 - 약간의 지연 추가
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(500);
                    return new BookPageGenerated(
                            "Generated page",
                            "https://image.png",
                            List.of("Question")
                    );
                });

        BookInitRequest pageRequest = new BookInitRequest(null, null, "Continue");
        String requestBody = objectMapper.writeValueAsString(pageRequest);

        // When - 서로 다른 책에 동시 요청
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
                        .andReturn()
                        .getResponse()
                        .getStatus();
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
                        .andReturn()
                        .getResponse()
                        .getStatus();
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

        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);

        // Then - 서로 다른 리소스이므로 둘 다 성공해야 함
        assertThat(completed).isTrue();
        assertThat(statusCodes).hasSize(2);
        assertThat(statusCodes).containsOnly(200);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/complete - 페이지 생성 중에 저장 요청 시 409 Conflict 응답")
    void completeBook_shouldReturn409Conflict_whenGenerateIsInProgress() throws Exception {
        // Given - 책 초기화
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest",
                testCharacterId,
                "Start"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // Mock 설정 - 페이지 생성에 시간이 걸리도록 설정 (2초)
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(2000);
                    return new BookPageGenerated(
                            "https://image.png",
                            "Generated page content",
                            List.of("Question")
                    );
                });

        BookInitRequest pageRequest = new BookInitRequest(null, null, "Continue story");
        String pageRequestBody = objectMapper.writeValueAsString(pageRequest);

        BookCompleteRequest completeRequest = new BookCompleteRequest(
                "Alice's Adventure",
                "Test Author"
        );
        String completeRequestBody = objectMapper.writeValueAsString(completeRequest);

        when(mockImageRepository.copyAllToPermanentStorage(any()))
                .thenReturn(Map.of(
                        "uploaded-image.jpg",
                        new ImageUploadResult("https://s3.amazonaws.com/temp/uploaded-image.jpg", "https://s3.amazonaws.com/uploaded-image.jpg")
                ));

        // When - 페이지 생성 요청과 저장 요청을 동시에 보냄
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);

        List<String> requestTypes = Collections.synchronizedList(new ArrayList<>());
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        // 첫 번째 스레드 - 페이지 생성 요청 (락 획득)
        Thread generateThread = new Thread(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(pageRequestBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                requestTypes.add("GENERATE");
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        // 두 번째 스레드 - 저장 요청 (락 충돌)
        Thread saveThread = new Thread(() -> {
            try {
                startLatch.await();
                Thread.sleep(100); // 페이지 생성 요청이 락을 먼저 획득하도록 약간의 지연
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}/complete", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(completeRequestBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                requestTypes.add("SAVE");
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        generateThread.start();
        saveThread.start();

        startLatch.countDown(); // 두 스레드 동시 시작

        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(statusCodes).hasSize(2);

        // 페이지 생성은 성공(200), 저장은 충돌(409)이어야 함
        int generateIndex = requestTypes.indexOf("GENERATE");
        int saveIndex = requestTypes.indexOf("SAVE");

        assertThat(statusCodes.get(generateIndex)).isEqualTo(200);
        assertThat(statusCodes.get(saveIndex)).isEqualTo(409);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id} - 저장 중에 페이지 생성 요청 시 409 Conflict 응답")
    void generatePage_shouldReturn409Conflict_whenSaveIsInProgress() throws Exception {
        // Given - 책 초기화
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest",
                testCharacterId,
                "Start"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // Mock 설정 - 이미지 업로드에 시간이 걸리도록 설정 (2초)
        when(mockImageRepository.copyAllToPermanentStorage(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(2000);
                    return Map.of(
                            "uploaded-image.jpg",
                            new ImageUploadResult("https://s3.amazonaws.com/temp/uploaded-image.jpg", "https://s3.amazonaws.com/uploaded-image.jpg")
                    );
                });

        BookCompleteRequest completeRequest = new BookCompleteRequest(
                "Alice's Adventure",
                "Test Author"
        );
        String completeRequestBody = objectMapper.writeValueAsString(completeRequest);

        // 페이지 생성 Mock (빠르게 완료)
        when(mockBookPageGenerator.generatePageFrom(any()))
                .thenReturn(new BookPageGenerated(
                        "https://image.png",
                        "Generated page content",
                        List.of("Question")
                ));

        BookInitRequest pageRequest = new BookInitRequest(null, null, "Continue story");
        String pageRequestBody = objectMapper.writeValueAsString(pageRequest);

        // When - 저장 요청과 페이지 생성 요청을 동시에 보냄
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);

        List<String> requestTypes = Collections.synchronizedList(new ArrayList<>());
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        // 첫 번째 스레드 - 저장 요청 (락 획득)
        Thread saveThread = new Thread(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}/complete", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(completeRequestBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                requestTypes.add("SAVE");
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        // 두 번째 스레드 - 페이지 생성 요청 (락 충돌)
        Thread generateThread = new Thread(() -> {
            try {
                startLatch.await();
                Thread.sleep(100); // 저장 요청이 락을 먼저 획득하도록 약간의 지연
                int status = mockMvc.perform(post("/api/v1/book/progress/{id}", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(pageRequestBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                requestTypes.add("GENERATE");
                statusCodes.add(status);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        saveThread.start();
        generateThread.start();

        startLatch.countDown(); // 두 스레드 동시 시작

        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(statusCodes).hasSize(2);

        // 저장은 성공(200), 페이지 생성은 충돌(409)이어야 함
        int saveIndex = requestTypes.indexOf("SAVE");
        int generateIndex = requestTypes.indexOf("GENERATE");

        assertThat(statusCodes.get(saveIndex)).isEqualTo(200);
        assertThat(statusCodes.get(generateIndex)).isEqualTo(409);
    }

    @Test
    @DisplayName("POST /api/v1/book/progress/{id}/complete - 저장 중에 또 다른 저장 요청 시 409 Conflict 응답")
    void completeBook_shouldReturn409Conflict_whenAnotherSaveIsInProgress() throws Exception {
        // Given - 책 초기화
        BookInitRequest initRequest = new BookInitRequest(
                "A magical forest",
                testCharacterId,
                "Start"
        );

        String initResponse = mockMvc.perform(post("/api/v1/book/progress/init")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bipId = objectMapper.readTree(initResponse)
                .path("data").path("bookInProgress").path("id").asText();

        // Mock 설정 - 이미지 업로드에 시간이 걸리도록 설정
        when(mockImageRepository.copyAllToPermanentStorage(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(2000);
                    return Map.of(
                            "uploaded-image.jpg",
                            new ImageUploadResult("https://s3.amazonaws.com/temp/uploaded-image.jpg", "https://s3.amazonaws.com/uploaded-image.jpg")
                    );
                });

        BookCompleteRequest completeRequest = new BookCompleteRequest(
                "Alice's Adventure",
                "Test Author"
        );
        String requestBody = objectMapper.writeValueAsString(completeRequest);

        // When - 두 개의 저장 요청을 동시에 보냄
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);

        MockHttpServletResponse[] response =  new MockHttpServletResponse[2];
        Thread completionRequest = new Thread(() -> {
            try {
                startLatch.await();
                response[0] = mockMvc.perform(post("/api/v1/book/progress/{id}/complete", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn()
                        .getResponse();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        Thread afterCompletionRequest = new Thread(() -> {
            try {
                startLatch.await();
                Thread.sleep(100);
                response[1] = mockMvc.perform(post("/api/v1/book/progress/{id}/complete", bipId)
                                .header("Authorization", "Bearer valid.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn()
                        .getResponse();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                completeLatch.countDown();
            }
        });

        completionRequest.start();
        afterCompletionRequest.start();
        startLatch.countDown();

        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(response[0].getStatus()).isEqualTo(200);
        assertThat(response[1].getStatus()).isEqualTo(409);
    }
}
