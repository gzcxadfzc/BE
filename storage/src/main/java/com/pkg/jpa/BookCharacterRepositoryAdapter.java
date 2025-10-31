package com.pkg.jpa;

import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterCreateCommand;
import com.pkg.domain.character.BookCharacterRepository;
import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookCharacterRepositoryAdapter implements BookCharacterRepository {

    private final CharacterJpaRepository repository;

    public BookCharacterRepositoryAdapter(CharacterJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public BookCharacter retrieveById(Long characterId) {
        CharacterJpaEntity entity = repository.findByCharacterId(characterId);
        if(entity == null) {
            return null;
        }
        return entity.toBookCharacter();
    }

    @Override
    public List<BookCharacter> retrieveByUser(Actor user) {
        List<CharacterJpaEntity> entities = repository.findByMemberId(user.id());
        return entities.stream()
                .map(CharacterJpaEntity::toBookCharacter)
                .toList();
    }

    @Override
    public BookCharacter createFrom(BookCharacterCreateCommand command) {
        CharacterJpaEntity entity = CharacterJpaEntity.builder()
                .appearanceKeywords(command.appearanceKeywords())
                .name(command.name())
                .personality(command.personality())
                .imageUrl(command.imageUrl())
                .originImageUrl(null)
                .userDescription(command.description())
                .memberId(command.userId())
                .build();
        return repository.save(entity).toBookCharacter();
    }
}
