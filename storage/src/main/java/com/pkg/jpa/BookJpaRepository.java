package com.pkg.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookJpaRepository extends JpaRepository<BookJpaEntity, String> {

    List<BookJpaEntity> findAllByUserId(Long userId);

    @Query("""
        SELECT new com.pkg.jpa.BookCharacterProjection(
            b.id,
            b.userId,
            b.title,
            b.author,
            c.id,
            c.name,
            c.appearanceKeywords,
            c.personality,
            c.userDescription,
            c.imageUrl
        )
        FROM BookJpaEntity b
        INNER JOIN CharacterJpaEntity c ON b.characterId = c.id
        WHERE b.id = :bookId
    """)
    BookCharacterProjection retrieveBookId(@Param("bookId")String bookId);

    Page<BookJpaEntity> findAll(Pageable pageable);

    Slice<BookJpaEntity> findSliceBy(Pageable pageable);
}
