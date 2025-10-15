package com.pkg.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "book_page")
public class PageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "book_id", nullable = false)
    private String bookId;
    @Column(name = "context", length = 511, nullable = false)
    private String context;
    @Column(name = "image_url", nullable = false)
    private String image_url;
    @Column(name = "page_number", nullable = false)
    private int pageNumber;
}
