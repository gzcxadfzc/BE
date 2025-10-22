package com.pkg.authentication.token;

import com.pkg.authentication.core.Credential;
import com.pkg.authentication.core.CredentialType;

public class AccessToken extends Credential {

    private final String value;

    public AccessToken(String token) {
        super(CredentialType.TOKEN);
        this.value = token;
    }

    public String getValue() {
        return value;
    }
}
