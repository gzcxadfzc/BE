package com.pkg.authentication.password;

import com.pkg.domain.member.Member;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberAuthenticationRepository {

    Member findByUsername(String username);
}
