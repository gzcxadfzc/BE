package com.pkg.domain.character;

import com.pkg.domain.member.Actor;
import com.pkg.domain.uitl.UuidGen;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookCharacterService {

    private final BookCharacterRepository characterRepository;
    private final CharacterInProgressRepository cipRepository;
    private final CharacterQueuePublisher queuePublisher;
    private final CharacterResultRepository resultRepository;

    public BookCharacterService(
            BookCharacterRepository characterRepository,
            CharacterInProgressRepository cipRepository,
            CharacterQueuePublisher queuePublisher,
            CharacterResultRepository resultRepository
    ) {
        this.characterRepository = characterRepository;
        this.cipRepository = cipRepository;
        this.queuePublisher = queuePublisher;
        this.resultRepository = resultRepository;
    }

    public BookCharacter retrieveById(Long characterId) {
        BookCharacter character = characterRepository.retrieveById(characterId);
        if (character == null) {
            throw BookCharacterException.notFound(characterId);
        }
        return character;
    }

    public List<BookCharacter> retrieveByUser(Actor currentUser) {
        return characterRepository.retrieveByUser(currentUser);
    }

    public String requestCreate(BookCharacterGenerateRequest request) {
        String cipId = UuidGen.compact();
        CharacterInProgress cip = CharacterInProgress.create(
                cipId,
                request.creator().id(),
                request.name(),
                request.appearanceKeywords(),
                request.personality(),
                request.description()
        );
        cipRepository.save(cip);
        queuePublisher.publish(CharacterQueueMessage.of(cipId, request));
        return cipId;
    }

    public CharacterPollResult pollStatus(Actor user, String cipId) {
        CharacterInProgress cip = cipRepository.getById(cipId);
        validateOwnership(user, cip);
        boolean resultExists = resultRepository.find(cipId).isPresent();
        return resultExists ? CharacterPollResult.ready() : CharacterPollResult.pending();
    }

    public BookCharacter completeCharacter(Actor user, String cipId) {
        CharacterInProgress cip = cipRepository.getById(cipId);
        validateOwnership(user, cip);
        CharacterResult result = resultRepository.find(cipId)
                .orElseThrow(() -> CharacterInProgressException.resultNotReady(cipId));
        BookCharacterCreateCommand command = cip.toCreateCommand(result.imageUrl());
        BookCharacter saved = characterRepository.createFrom(command);
        resultRepository.delete(cipId);
        cipRepository.markAsCompleted(cipId);
        return saved;
    }

    private void validateOwnership(Actor user, CharacterInProgress cip) {
        if (!cip.userId().equals(user.id())) {
            throw CharacterInProgressException.forbidden();
        }
    }
}
