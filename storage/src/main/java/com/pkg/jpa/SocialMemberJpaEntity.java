package com.pkg.jpa;

import jakarta.persistence.*;

@Entity
@IdClass(SocialMemberPK.class)
@Table(name = "social_member")
public class SocialMemberJpaEntity {
    @Id
    @Column(name = "auth_provider")
    private String authProvider;
    @Id
    @Column(name = "provided_id")
    private Long providedId;
    private String email;
    private String nickName;
    @ManyToOne
    @JoinColumn(name = "member_id")
    private MemberJpaEntity member;
}
