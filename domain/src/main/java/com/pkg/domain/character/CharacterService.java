package com.pkg.domain.character;

import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacterService {

    private final CharacterRepository characterRepository;

    public CharacterService(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    public BookCharacter retrieveById(String characterId) {
        BookCharacter character = characterRepository.retrieveById(characterId);
        if(character == null) {
            throw BookCharacterException.notFound(characterId);
        }
        return character;
    }

    public List<BookCharacter> retrieveByUser(Actor currentUser) {
        return characterRepository.retrieveByUser(currentUser);
    }
}
