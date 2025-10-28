package com.pkg.domain.character;

import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookCharacterService {

    private final BookCharacterRepository characterRepository;

    public BookCharacterService(BookCharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    public BookCharacter retrieveById(Long characterId) {
        BookCharacter character = characterRepository.retrieveById(characterId);
        if(character == null) {
            throw BookCharacterException.notFound(characterId);
        }
        return character;
    }

    public BookCharacter create(BookCharacterCreateCommand command) {
        return characterRepository.save(command);
    }

    public List<BookCharacter> retrieveByUser(Actor currentUser) {
        return characterRepository.retrieveByUser(currentUser);
    }
}
