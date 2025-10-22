package com.pkg.controller.auth;

import com.pkg.authentication.password.PasswordAuthenticator;
import com.pkg.authentication.password.UsernamePassword;
import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.authentication.token.TokenProvider;
import com.pkg.controller.common.ApiResponse;
import com.pkg.domain.member.Member;
import com.pkg.domain.member.MemberService;
import com.pkg.domain.member.SignUpCommand;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final PasswordAuthenticator passwordAuthenticator;
    private final MemberService memberService;
    private final TokenProvider tokenProvider;

    public AuthController(PasswordAuthenticator passwordAuthenticator, MemberService memberService, TokenProvider tokenProvider) {
        this.passwordAuthenticator = passwordAuthenticator;
        this.memberService = memberService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/signin")
    public ApiResponse<SignInResponse> signIn(@RequestBody SignInRequest request) {
        UsernamePassword usernamePassword = UsernamePassword.of(request.username(), request.password());
        MemberPrincipal memberPrincipal = passwordAuthenticator.authenticate(usernamePassword);
        AccessToken token = tokenProvider.issue(memberPrincipal);
        return ApiResponse.success(new SignInResponse(token.getValue()));
    }

    @PostMapping("/signup")
    public ApiResponse<SignInResponse> singUp(@RequestBody SignUpRequest request) {
        Member member = memberService.signUp(new SignUpCommand(request.username(), request.password()));
        AccessToken token = tokenProvider.issue(member);
        return ApiResponse.success(new SignInResponse(token.getValue()));
    }
}
