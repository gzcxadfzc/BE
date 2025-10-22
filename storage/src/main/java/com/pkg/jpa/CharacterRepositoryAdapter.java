package com.pkg.jpa;

import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterCreationRequest;
import com.pkg.domain.character.CharacterRepository;
import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CharacterRepositoryAdapter implements CharacterRepository {

    public CharacterRepositoryAdapter(CharacterJpaRepository repository) {
        this.repository = repository;
    }

    private final CharacterJpaRepository repository;

    @Override
    public BookCharacter retrieveById(String characterId) {
        return null;
    }

    @Override
    public List<BookCharacter> retrieveByUser(Actor user) {
        return List.of();
    }

    @Override
    public BookCharacter save(BookCharacterCreationRequest request) {
        return null;
    }
}
