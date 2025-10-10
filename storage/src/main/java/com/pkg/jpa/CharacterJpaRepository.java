package com.pkg.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterJpaRepository extends JpaRepository<CharacterJpaEntity, Long> {

    Page<CharacterJpaEntity> findByMemberId(Long memberId, Pageable pageable);
}
