package com.pkg.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialUserJpaRepository extends JpaRepository<SocialMemberJpaEntity, String> {
    Boolean existsByAuthProviderAndProvidedId(String authProvider, Long providedId);
    SocialMemberJpaEntity findByAuthProviderAndProvidedId(String authProvider, Long providedId);
}
