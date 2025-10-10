package com.pkg.domain.character;

public interface CharacterRepository {

    MainCharacter retrieveByCharacterId(String characterId);

    MainCharacter retrieveByMemberId(String memberId);

    void save(MainCharacter mainCharacter);
}
