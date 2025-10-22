package com.pkg.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageJpaRepository extends JpaRepository<PageJpaEntity, Long> {

        List<PageJpaEntity> findAllByBookId(String bookId);

        PageJpaEntity findByBookId(String id);
}
