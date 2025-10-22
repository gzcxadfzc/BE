package com.pkg.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "social_member")
public class SocialMemberJpaEntity {

    @Id
    @Column(name = "auth_provider", nullable = false)
    private String authProvider;
    @Id
    @Column(name = "provided_id", nullable = false)
    private Long providedId;
    private String email;
    private String nickName;
    @ManyToOne
    @JoinColumn(name = "member_id")
    private MemberJpaEntity member;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String authProvider;
        private Long providedId;
        private String email;
        private String nickName;
        private MemberJpaEntity member;

        public Builder authProvider(String authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder providedId(Long providedId) {
            this.providedId = providedId;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder nickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public Builder member(MemberJpaEntity member) {
            this.member = member;
            return this;
        }

        public SocialMemberJpaEntity build() {
            SocialMemberJpaEntity socialMember = new SocialMemberJpaEntity();
            socialMember.authProvider = this.authProvider;
            socialMember.providedId = this.providedId;
            socialMember.email = this.email;
            socialMember.nickName = this.nickName;
            socialMember.member = this.member;
            return socialMember;
        }
    }
}
