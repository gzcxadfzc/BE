package com.pkg.authentication.core;

public abstract class Credential {

    private final CredentialType authenticationType;

    protected Credential(CredentialType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public CredentialType getAuthenticationType() {
        return authenticationType;
    }
}
