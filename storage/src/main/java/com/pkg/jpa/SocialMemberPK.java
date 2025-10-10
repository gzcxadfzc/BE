package com.pkg.jpa;

import java.io.Serializable;

public class SocialMemberPK implements Serializable {

    private String authProvider;
    private Long providedId;

    public String getAuthProvider() {
        return authProvider;
    }

    public Long getProvidedId() {
        return providedId;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public void setProvidedId(Long providedId) {
        this.providedId = providedId;
    }

    public SocialMemberPK(String authProvider, Long providedId) {
        this.authProvider = authProvider;
        this.providedId = providedId;
    }
}
