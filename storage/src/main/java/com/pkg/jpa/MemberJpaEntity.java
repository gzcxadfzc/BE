package com.pkg.jpa;

import com.pkg.domain.member.Member;
import com.pkg.domain.member.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "member")
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    @Column(name = "password")
    private String password;
    @Enumerated(EnumType.STRING)
    private RoleJpa role;
    @Column(name = "auth_provider")
    private String authProvider;

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public Member toMember() {
        return new Member(
                id,
                username,
                password,
                Role.ofName(role.name())
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String password;
        private RoleJpa role;
        private String authProvider;

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

        public Builder role(RoleJpa role) {
            this.role = role;
            return this;
        }

        public Builder authProvider(String authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public MemberJpaEntity build() {
            MemberJpaEntity member = new MemberJpaEntity();
            member.id = this.id;
            member.username = this.username;
            member.password = this.password;
            member.role = this.role;
            member.authProvider = this.authProvider;
            return member;
        }
    }
}
