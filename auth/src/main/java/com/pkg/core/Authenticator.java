package com.pkg.core;

public interface Authenticator<C extends Credential, P extends AuthenticationPrincipal> {

    P authenticate(C credential);
}
