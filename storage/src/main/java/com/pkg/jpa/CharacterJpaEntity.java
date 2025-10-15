package com.pkg.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "main_character")
public class CharacterJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "personality", nullable = false)
    private String personality;
    @Column(name = "image_url", nullable = false)
    private String imageUrl;
    @Column(name = "original_image_url", nullable = false)
    private String originImageUrl;
    @Column(name = "user_description", nullable = false)
    private String userDescription;
    @Column(name = "appearance_keywords", nullable = false)
    private String appearanceKeywords;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
}
