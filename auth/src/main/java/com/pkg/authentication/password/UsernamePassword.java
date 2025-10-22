package com.pkg.authentication.password;

import com.pkg.authentication.core.AuthenticationException;
import com.pkg.authentication.core.Credential;
import com.pkg.authentication.core.CredentialType;

import java.util.Objects;

public class UsernamePassword extends Credential{

    private final String username;
    private final String password;

    private UsernamePassword(CredentialType authenticationType, String username, String password) {
        super(authenticationType);
        this.username = username;
        this.password = password;
    }

    public static UsernamePassword of(String username, String password) {
        validate(username, password);
        return new UsernamePassword(CredentialType.USERNAME_PASSWORD, username, password);
    }

    private static void validate(String username, String password) {
        if(username.isEmpty()) {
            throw AuthenticationException.invalidCredential("username must not be blank");
        }
        if(password.isEmpty()) {
            throw UsernamePasswordException.invalidCredential("password must not be blank");
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsernamePassword that = (UsernamePassword) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
