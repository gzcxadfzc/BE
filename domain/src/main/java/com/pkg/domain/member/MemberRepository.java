package com.pkg.domain.member;

import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository {

    Member register(Member member);
}
