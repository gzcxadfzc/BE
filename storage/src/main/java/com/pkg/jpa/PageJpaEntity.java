package com.pkg.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "book_page")
public class PageJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String bookId;
    @Column(length = 511)
    private String context;
    private String colorImageUrl;
    private String sketchImageUrl;
    private String actionInfo;
    private int pageNumber;
}
