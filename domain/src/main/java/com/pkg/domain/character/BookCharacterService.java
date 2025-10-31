package com.pkg.domain.character;

import com.pkg.domain.ai.BookCharacterGenerator;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookCharacterService {

    private final BookCharacterRepository characterRepository;
    private final BookCharacterGenerator generator;
    private final ImageRepository imageRepository;

    public BookCharacterService(
            BookCharacterRepository characterRepository,
            BookCharacterGenerator generator,
            ImageRepository imageRepository
    ) {
        this.characterRepository = characterRepository;
        this.generator = generator;
        this.imageRepository = imageRepository;
    }

    public BookCharacter retrieveById(Long characterId) {
        BookCharacter character = characterRepository.retrieveById(characterId);
        if(character == null) {
            throw BookCharacterException.notFound(characterId);
        }
        return character;
    }

    public BookCharacter create(BookCharacterGenerateRequest request) {
        String imageUrl = generator.generateImageFrom(request);
        ImageUploadResult result = imageRepository.uploadCharacterImage(imageUrl);
        BookCharacterCreateCommand command = request.toCommand(result.newUrl());
        return characterRepository.createFrom(command);
    }

    public List<BookCharacter> retrieveByUser(Actor currentUser) {
        return characterRepository.retrieveByUser(currentUser);
    }
}
