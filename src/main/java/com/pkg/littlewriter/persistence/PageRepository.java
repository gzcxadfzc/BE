package com.pkg.littlewriter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    List<PageEntity> getAllByBookId(String bookId);
    PageEntity getById(Long id);
}
