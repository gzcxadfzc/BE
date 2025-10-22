package com.pkg.domain.member;

public record Member(
    Long id,
    String username,
    String password,
    Role role
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long id;
        private String username;
        private String password;
        private Role role;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder Role(Role role) {
            this.role = role;
            return this;
        }

        public Member build() {
            return new Member(
                id,
                username,
                password,
                role
            );
        }
    }
}
