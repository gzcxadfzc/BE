package com.pkg.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookPageJpaRepository extends JpaRepository<BookJpaEntity, String> {
    Optional<BookJpaEntity> findById(String bookId);
    Page<BookJpaEntity> findAllByUserId(Long userId, Pageable pageable);
    List<BookJpaEntity> findAllByUserId(Long userId);
    List<BookJpaEntity> findAllByCharacterId(Long characterId);
}
