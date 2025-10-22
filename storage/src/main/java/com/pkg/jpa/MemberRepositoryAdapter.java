package com.pkg.jpa;

import com.pkg.domain.member.Member;
import com.pkg.domain.member.MemberException;
import com.pkg.domain.member.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository repository;

    public MemberRepositoryAdapter(MemberJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Member register(Member usernamePassword) {
        try {
            MemberJpaEntity entity = repository.save(
                    MemberJpaEntity.builder()
                            .role(RoleJpa.MEMBER)
                            .password(usernamePassword.password())
                            .username(usernamePassword.username())
                            .build());
            return entity.toMember();
        } catch (DataIntegrityViolationException e) {
            Throwable root = e.getMostSpecificCause();
            String message = root.getMessage();
            if(message != null && message.contains("username")) {
                throw MemberException.duplicatedUsername(usernamePassword.username());
            }
            throw e;
        }
    }
}
