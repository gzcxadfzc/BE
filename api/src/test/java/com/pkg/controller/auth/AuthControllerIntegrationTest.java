package com.pkg.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.TestApplication;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test-in-memory")
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Clean up database
        memberJpaRepository.deleteAll();
        entityManager.clear();
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - 새로운 사용자를 등록할 수 있다")
    void signUp_shouldCreateNewUser_andReturnToken() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("newuser", "password123");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - 중복된 사용자명으로 회원가입 시 예외가 발생한다")
    void signUp_shouldThrowException_whenUsernameAlreadyExists() throws Exception {
        // given - 기존 사용자 생성
        MemberJpaEntity existingMember = MemberJpaEntity.builder()
                .username("existinguser")
                .password("hashed_password")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(existingMember);

        SignUpRequest request = new SignUpRequest("existinguser", "newpassword123");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signin - 올바른 자격증명으로 로그인할 수 있다")
    void signIn_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        // given - 먼저 사용자 등록
        SignUpRequest signUpRequest = new SignUpRequest("testuser", "password123");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        SignInRequest signInRequest = new SignInRequest("testuser", "password123");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signin - 잘못된 비밀번호로 로그인 시 예외가 발생한다")
    void signIn_shouldThrowException_whenPasswordIsIncorrect() throws Exception {
        // given - 먼저 사용자 등록
        SignUpRequest signUpRequest = new SignUpRequest("testuser", "password123");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        SignInRequest signInRequest = new SignInRequest("testuser", "wrongpassword");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signin - 존재하지 않는 사용자명으로 로그인 시 예외가 발생한다")
    void signIn_shouldThrowException_whenUsernameNotFound() throws Exception {
        // given
        SignInRequest signInRequest = new SignInRequest("nonexistentuser", "password123");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - 빈 사용자명으로 회원가입 시 예외가 발생한다")
    void signUp_shouldThrowException_whenUsernameIsEmpty() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("", "password123");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - 빈 비밀번호로 회원가입 시 예외가 발생한다")
    void signUp_shouldThrowException_whenPasswordIsEmpty() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("validuser", "");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signin - 빈 사용자명으로 로그인 시 예외가 발생한다")
    void signIn_shouldThrowException_whenUsernameIsEmpty() throws Exception {
        // given
        SignInRequest signInRequest = new SignInRequest("", "password123");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signin - 빈 비밀번호로 로그인 시 예외가 발생한다")
    void signIn_shouldThrowException_whenPasswordIsEmpty() throws Exception {
        // given
        SignInRequest signInRequest = new SignInRequest("testuser", "");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - 회원가입 후 반환된 토큰으로 인증된 요청을 할 수 있다")
    void signUp_shouldReturnValidToken_thatCanBeUsedForAuthentication() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("tokentest", "password123");

        // when - 회원가입
        String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 토큰 추출 및 인증된 요청 테스트
        String token = objectMapper.readTree(response)
                .get("data")
                .get("accessToken")
                .asText();

        mockMvc.perform(get("/api/v1/book/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - 회원가입 후 비밀번호는 Argon2형식으로 hashed되어 저장되어야 한다.")
    void signUp_shouldStorePasswordArgon2() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("tokentest", "password123");
        Pattern argon2Pattern = Pattern.compile(
                "^\\$argon2(id|i|d)\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+$"
        );
        // when - 회원가입
        String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 비밀번호 평문 저장 여부 확인
        String hashed = memberJpaRepository.getByUsername("tokentest").getPassword();
        assertThat(argon2Pattern.matcher(hashed).matches()).isTrue();
        assertThat(hashed).isNotEqualTo("password123");
    }
}
