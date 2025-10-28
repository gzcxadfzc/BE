package com.pkg.domain.character;

import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCharacterRepository {

    BookCharacter retrieveById(Long characterId);

    List<BookCharacter> retrieveByUser(Actor user);

    BookCharacter save(BookCharacterCreateCommand command);
}
