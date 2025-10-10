package com.pkg.authentication.guest;

import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.core.Authenticator;
import org.springframework.stereotype.Component;

@Component
public class GuestIdAuthenticator implements Authenticator<GuestId, MemberPrincipal> {

    @Override
    public MemberPrincipal authenticate(GuestId credential) {
        return null;
    }
}
