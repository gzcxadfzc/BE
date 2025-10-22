package com.pkg.jpa;

import com.pkg.authentication.password.MemberAuthenticationRepository;
import com.pkg.domain.member.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberAuthenticationRepositoryAdapter implements MemberAuthenticationRepository {

    private final MemberJpaRepository repository;

    public MemberAuthenticationRepositoryAdapter(MemberJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Member findByUsername(String username) {
        MemberJpaEntity memberJpaEntity = repository.getByUsername(username);
        if(memberJpaEntity == null) {
            return null;
        }
        return memberJpaEntity.toMember();
    }
}
