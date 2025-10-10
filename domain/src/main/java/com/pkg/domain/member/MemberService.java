package com.pkg.domain.member;

import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private MemberRepository repository;

    public MemberService(MemberRepository repository) {
        this.repository = repository;
    }


}
