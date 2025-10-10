package com.pkg.authentication.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.pkg.configs.JwtConfig;
import com.pkg.domain.member.Member;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    private final Algorithm privateKeyAlgorithm;
    private final JwtConfig accessTokenConfig;

    public TokenProvider(
            @Qualifier("jwtPrivateKeyAlgorithm") Algorithm privateKeyAlgorithm,
            @Qualifier("accessTokenConfig") JwtConfig accessTokenConfig
    ) {
        this.privateKeyAlgorithm = privateKeyAlgorithm;
        this.accessTokenConfig = accessTokenConfig;
    }

    public AccessToken issue(MemberPrincipal memberPrincipal) {
        return new AccessToken(
                JWT.create()
                        .withIssuer(accessTokenConfig.getIssuer())
                        .withClaim(JwtConfig.Claims.MEMBER_NO.getName(), memberPrincipal.getMemberId())
                        .withClaim(JwtConfig.Claims.ROLE.getName(), memberPrincipal.getRole().name())
                        .sign(privateKeyAlgorithm)
        );
    }

    public AccessToken issue(Member member) {
        return new AccessToken(
                JWT.create()
                        .withIssuer(accessTokenConfig.getIssuer())
                        .withClaim(JwtConfig.Claims.MEMBER_NO.getName(), member.id())
                        .withClaim(JwtConfig.Claims.ROLE.getName(), member.role().name())
                        .sign(privateKeyAlgorithm)
        );
    }
}
