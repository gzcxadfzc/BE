package com.pkg.domain.character;

import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository {

    BookCharacter retrieveById(String characterId);

    List<BookCharacter> retrieveByUser(Actor user);

    BookCharacter save(BookCharacterCreationRequest request);
}
