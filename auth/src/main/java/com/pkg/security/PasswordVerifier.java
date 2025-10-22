package com.pkg.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordVerifier {

    private final Argron2PasswordUtil passwordUtil;

    public PasswordVerifier(Argron2PasswordUtil passwordUtil) {
        this.passwordUtil = passwordUtil;
    }

    public boolean verify(String password, String hash) {
        return passwordUtil.verify(password, hash);
    };
}
