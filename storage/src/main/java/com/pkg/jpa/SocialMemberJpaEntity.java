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
}
