package com.pkg.domain.member;

import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final PasswordHasher passwordHasher;
    private final MemberRepository repository;

    public MemberService(PasswordHasher passwordHasher, MemberRepository repository) {
        this.passwordHasher = passwordHasher;
        this.repository = repository;
    }

    public Member signUp(SignUpCommand signUpCommand) {
        Member member = Member.builder()
                .username(signUpCommand.username())
                .password(passwordHasher.hash(signUpCommand.password()))
                .Role(Role.MEMBER)
                .build();
        return repository.register(member);
    }
}
