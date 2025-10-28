package com.pkg.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterJpaRepository extends JpaRepository<CharacterJpaEntity, Long> {

    @Query("SELECT c FROM CharacterJpaEntity c WHERE c.id = :id")
    CharacterJpaEntity findByCharacterId(@Param("id")Long id);

    List<CharacterJpaEntity> findByMemberId(Long memberId);
}
