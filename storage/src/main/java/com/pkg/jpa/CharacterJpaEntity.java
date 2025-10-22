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
    @Column(name = "origin_image_url", nullable = false)
    private String originImageUrl;
    @Column(name = "user_description", nullable = false)
    private String userDescription;
    @Column(name = "appearance_keywords", nullable = false)
    private String appearanceKeywords;
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public Long getId() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String personality;
        private String imageUrl;
        private String originImageUrl;
        private String userDescription;
        private String appearanceKeywords;
        private Long memberId;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder personality(String personality) {
            this.personality = personality;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder originImageUrl(String originImageUrl) {
            this.originImageUrl = originImageUrl;
            return this;
        }

        public Builder userDescription(String userDescription) {
            this.userDescription = userDescription;
            return this;
        }

        public Builder appearanceKeywords(String appearanceKeywords) {
            this.appearanceKeywords = appearanceKeywords;
            return this;
        }

        public Builder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public CharacterJpaEntity build() {
            CharacterJpaEntity character = new CharacterJpaEntity();
            character.id = this.id;
            character.name = this.name;
            character.personality = this.personality;
            character.imageUrl = this.imageUrl;
            character.originImageUrl = this.originImageUrl;
            character.userDescription = this.userDescription;
            character.appearanceKeywords = this.appearanceKeywords;
            character.memberId = this.memberId;
            return character;
        }
    }
}
