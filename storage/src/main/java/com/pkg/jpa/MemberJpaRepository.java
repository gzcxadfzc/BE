package com.pkg.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, String> {
    MemberJpaEntity findById(Long id);
    MemberJpaEntity findByMemberName(String memberName);
    Boolean existsByMemberName(String memberName);
}
