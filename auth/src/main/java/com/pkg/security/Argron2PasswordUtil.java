package com.pkg.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Component;

@Component
public class Argron2PasswordUtil  {

    private static final int ITERATIONS = 2;
    private static final int MEMORY_KB = 32768;     // 32MB
    private static final int PARALLELISM = 2;
    private static final Argon2 ARGON_2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    public String hash(String password) {
        return ARGON_2.hash(ITERATIONS,  MEMORY_KB, PARALLELISM, password.toCharArray());
    }

    public boolean verify(String password, String hash) {
        return ARGON_2.verify(hash, password.toCharArray());
    }
}
