package com.pkg.domain.member;

import org.springframework.stereotype.Component;

@Component
public interface PasswordHasher {

    String hash(String password);
}
