package com.pkg.domain.member;

public record SignUpCommand (
        String username,
        String password
) {

    public SignUpCommand(String username, String password) {
        validate(username, password);
        this.username = username;
        this.password = password;
    }

    private void validate(String username, String password) {
        if(username.isEmpty()) {
            throw MemberException.invalidCommand("username must not be blank");
        }
        if(password.isEmpty()) {
            throw MemberException.invalidCommand("password must not be blank");
        }
    }
}
