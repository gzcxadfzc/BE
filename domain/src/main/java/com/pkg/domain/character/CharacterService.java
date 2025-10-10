package com.pkg.domain.character;

import org.springframework.stereotype.Service;

@Service
public class CharacterService {

    private CharacterRepository characterRepository;
    private CharacterGenerator characterGenerator;

    public CharacterService(CharacterRepository characterRepository, CharacterGenerator characterGenerator) {
        this.characterRepository = characterRepository;
        this.characterGenerator = characterGenerator;
    }

    MainCharacter createCharacterFrom(CharacterOrigin characterOrigin) {
        MainCharacter character = characterGenerator.generate(characterOrigin);
        characterRepository.save(character);
        return character;
    }
}
