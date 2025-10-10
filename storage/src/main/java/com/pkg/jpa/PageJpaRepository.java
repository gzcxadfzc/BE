package com.pkg.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageJpaRepository extends JpaRepository<PageJpaEntity, Long> {

    List<PageJpaEntity> getAllByBookId(String bookId);
    PageJpaEntity getById(Long id);
}
