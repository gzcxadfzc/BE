package com.pkg;

import com.pkg.authentication.token.*;
import com.pkg.authentication.core.AuthenticationExceptionType;
import com.pkg.support.RequestAttributeKey;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.pkg.AuthTestConfig.*;

@ActiveProfiles("test")
@WebMvcTest
@ContextConfiguration(classes = {
        AuthTestConfig.DummyController.class,
        AuthTestConfig.TestControllerAdvice.class,
        AuthTestConfig.TestConfig.class
})
@DisplayName("JwtAuthentication 테스트")
class JwtAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    FilterChain filterChain;

    @Test
    @DisplayName("유효한 Bearer 토큰이 있을 때 인증이 성공해야 한다")
    void shouldAuthenticateSuccessfully_whenValidBearerTokenProvided() throws Exception {
        // given
        MockHttpServletRequestBuilder mockRequest =
                MockMvcRequestBuilders.get("/secure")
                        .header("Authorization", "Bearer " + VALID_ADMIN_JWT);
        // when & then
        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(result -> {
                    MemberPrincipal principal = (MemberPrincipal) result.getRequest().getAttribute(RequestAttributeKey.PRINCIPAL.name());
                    assertThat(principal).isEqualTo(VALID_ADMIN_PRINCIPAL);
                })
                .andExpect(jsonPath("$.id").value(VALID_ADMIN_PRINCIPAL.getMemberId().toString()))
                .andExpect(jsonPath("$.role").value(VALID_ADMIN_PRINCIPAL.getRole().toString()))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 없을 때 예외가 발생해야 한다")
    void shouldThrowException_whenAuthorizationHeaderMissing() throws Exception {
        // given
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders.get("/secure");

        // when & then
        mockMvc.perform(mockRequest) // 헤더 미설정
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value(AuthenticationExceptionType.MISSING_CREDENTIAL.name()))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer로 시작하지 않을 때 예외가 발생해야 한다")
    void shouldThrowException_whenAuthorizationHeaderNotStartsWithBearer() throws Exception {
        // given
        MockHttpServletRequestBuilder mockRequest =
                MockMvcRequestBuilders.get("/secure")
                .header("Authorization", "NotBearer " + VALID_MEMBER_JWT);

        // when & then
        mockMvc.perform(mockRequest) // 헤더 미설정
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value(AuthenticationExceptionType.MISSING_CREDENTIAL.name()))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 비어있을 때 예외가 발생해야 한다")
    void shouldThrowException_whenAuthorizationHeaderEmpty() throws Exception {
        // given
        MockHttpServletRequestBuilder mockRequest =
                MockMvcRequestBuilders.get("/secure")
                        .header("Authorization", "");

        // when & then
        mockMvc.perform(mockRequest) // 헤더 미설정
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value(AuthenticationExceptionType.MISSING_CREDENTIAL.name()))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()));
        verify(filterChain, never()).doFilter(any(), any());
    }


    @Test
    @DisplayName("shouldNotFilter가 아닌 URI에 대해 Bearer Token이 없어도 예외처리 되지 않아야 한다.")
    void shouldNotFilter_whenUriIsInExclusionList() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders.get("/public/any");

        mockMvc.perform(mockRequest) // 헤더 미설정
                .andExpect(status().isOk())
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()));

        verify(filterChain, never()).doFilter(any(), any());
    }
}
