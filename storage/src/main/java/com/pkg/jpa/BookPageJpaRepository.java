package com.pkg.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookPageJpaRepository extends JpaRepository<BookPageJpaEntity, Long> {

        List<BookPageJpaEntity> findAllByBookId(String bookId);

        BookPageJpaEntity findByBookId(String id);
}
