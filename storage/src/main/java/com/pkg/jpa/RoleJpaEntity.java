package com.pkg.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class RoleJpaEntity {

    @Id
    private int id;
    private String name;
}
