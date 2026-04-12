package com.pkg.domain.character;

public interface CharacterInProgressRepository {
    void save(CharacterInProgress cip);
    CharacterInProgress getById(String cipId);
    void markAsCompleted(String cipId);
}
