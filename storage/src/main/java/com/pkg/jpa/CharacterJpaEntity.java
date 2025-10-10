package com.pkg.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "main_character")
public class CharacterJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String personality;
    private String imageUrl;
    private String originImageUrl;
    private String userDescription;
    private String appearanceKeywords;
    @Column(nullable = false)
    private Long memberId;
}
