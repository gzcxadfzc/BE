package com.pkg.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="book_create_status")
public class BookCreateStatusJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private int lastPageNo = 0;

    @Column(nullable = false)
    private int totalPages = 0;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        CREATING, DONE, EXPIRED
    }
}
