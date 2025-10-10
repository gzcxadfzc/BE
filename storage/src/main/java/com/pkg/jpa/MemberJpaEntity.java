package com.pkg.jpa;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "member")
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String username;
    private String password;
    private String authority;
    private String authProvider;
    @OneToMany(mappedBy = "member")
    private List<SocialMemberJpaEntity> socialMemberEntities;

    @PrePersist
    private void setDefaultPrivilege() {
        if(this.authority == null) {
            this.authority = "standard";
        }
    }
}
