package com.pkg.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CharacterJpaRepository extends JpaRepository<CharacterJpaEntity, Long> {

    Optional<CharacterJpaEntity> findById(Long  characterId);
}
