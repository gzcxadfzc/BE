package com.pkg.domain.character;

import java.util.Optional;

public interface CharacterResultRepository {
    Optional<CharacterResult> find(String characterId);
    void delete(String characterId);
}
