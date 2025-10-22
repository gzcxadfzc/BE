package com.pkg.security;

import com.pkg.domain.member.PasswordHasher;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasherAdapter implements PasswordHasher {

    private final Argron2PasswordUtil argron2PasswordUtil;

    public PasswordHasherAdapter(Argron2PasswordUtil argron2PasswordUtil) {
        this.argron2PasswordUtil = argron2PasswordUtil;
    }

    @Override
    public String hash(String password) {
        return argron2PasswordUtil.hash(password);
    }
}
