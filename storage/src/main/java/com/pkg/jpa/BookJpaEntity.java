package com.pkg.jpa;

import jakarta.persistence.*;

import java.util.Date;


@Entity
@Table(name = "book")
public class BookJpaEntity {

    @Id
    private String id;
    private Long userId;
    private Long characterId;
    private String title;
    @Temporal(TemporalType.TIMESTAMP)

    private Date createDate;
    private Long bookColor;
    private String author;
    private int storyLength;
    private String coverImageUrl;
}
