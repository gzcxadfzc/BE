package com.pkg.core;

public abstract class AuthenticationPrincipal {

    protected final String identifier;

    protected AuthenticationPrincipal(String id) {
        this.identifier = id;
    }
}
