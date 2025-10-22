package com.pkg.authentication.password;

import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.authentication.core.Authenticator;
import com.pkg.domain.member.Member;
import com.pkg.security.PasswordVerifier;
import org.springframework.stereotype.Component;

@Component
public class PasswordAuthenticator implements Authenticator<UsernamePassword, MemberPrincipal> {

    private final MemberAuthenticationRepository repository;
    private final PasswordVerifier verifier;

    public PasswordAuthenticator(MemberAuthenticationRepository memberAuthenticationRepository, PasswordVerifier passwordHasher) {
        this.repository = memberAuthenticationRepository;
        this.verifier = passwordHasher;
    }

    @Override
    public MemberPrincipal authenticate(UsernamePassword credential) {
        Member member = repository.findByUsername(credential.getUsername());
        if(member == null) {
            throw UsernamePasswordException.notFound(credential);
        }
        boolean isValidate = verifier.verify(credential.getPassword(), member.password());
        if(!isValidate) {
            throw UsernamePasswordException.wrongPassword("wrong password");
        }
        return new MemberPrincipal(member.id(), member.role());
    }
}
