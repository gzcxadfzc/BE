package com.pkg.authentication.token;

import com.pkg.core.Authenticator;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenAuthenticator implements Authenticator<AccessToken, MemberPrincipal> {

    private final AccessTokenValidator memberTokenValidator;

    public AccessTokenAuthenticator(AccessTokenValidator memberTokenValidator) {
        this.memberTokenValidator = memberTokenValidator;
    }

    @Override
    public com.pkg.authentication.token.MemberPrincipal authenticate(AccessToken token) {
        AccessTokenPayload payload = memberTokenValidator.getPayload(token);
        return MemberPrincipal.fromPayload(payload);
    }
}

