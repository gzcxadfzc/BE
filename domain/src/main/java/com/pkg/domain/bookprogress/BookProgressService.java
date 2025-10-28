package com.pkg.domain.bookprogress;

import com.pkg.domain.ai.BookPageGenerated;
import com.pkg.domain.ai.BookPageGenerator;
import com.pkg.domain.ai.CreateOnePageCommand;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.book.BookRepository;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterRepository;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import com.pkg.domain.member.Actor;
import com.pkg.domain.member.Role;
import com.pkg.domain.uitl.UuidGen;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class BookProgressService {

    private final BookCharacterRepository bookCharacterRepository;
    private final BookRepository bookRepository;
    private final BookInProgressRepository bookInProgressRepository;
    private final BookPageGenerator bookPageGenerator;
    private final ImageRepository imageRepository;

    public BookProgressService(
            BookCharacterRepository bookCharacterRepository,
            BookRepository bookRepository,
            BookInProgressRepository bookInProgressRepository,
            BookPageGenerator bookPageGenerator,
            ImageRepository imageRepository
    ) {
        this.bookCharacterRepository = bookCharacterRepository;
        this.bookRepository = bookRepository;
        this.bookInProgressRepository = bookInProgressRepository;
        this.bookPageGenerator = bookPageGenerator;
        this.imageRepository = imageRepository;
    }

    public AiGenerateResult initBook(BookInitCommand request) {
        BookCharacter bookCharacter = bookCharacterRepository.retrieveById(request.characterId());
        if(bookCharacter == null) {
            throw BookProgressException.notFound("not found bookCharacter :" + request.characterId());
        }
        BookInProgress bookInProgress = new BookInProgress(
                UuidGen.compact(),
                request.currentUser().id(),
                request.background(),
                bookCharacter,
                new ArrayList<>()
        );
        bookInProgressRepository.save(bookInProgress);
        BookPageGenerated bookPageGenerated = bookPageGenerator.generatePageFrom(new BookToProgress(bookInProgress, request.userInput()));
        ImageUploadResult result = imageRepository.uploadTemporary(bookPageGenerated.generatedIllustrationUrl());
        BookInProgress updated = bookInProgressRepository.addPageTo(bookInProgress.id(), new BookPage(bookPageGenerated.context(), result.newUrl(), 0));
        return new AiGenerateResult(updated, bookPageGenerated.questions());
    }

    public AiGenerateResult generateWithAi(CreateOnePageCommand createOnePageRequest) {
        BookInProgress bip = bookInProgressRepository.retrieveById(createOnePageRequest.bipId());
        validateOwner(bip, createOnePageRequest.currentUser());
        BookPageGenerated bookPageGenerated = bookPageGenerator.generatePageFrom(new BookToProgress(bip, createOnePageRequest.userInput()));
        ImageUploadResult result = imageRepository.uploadTemporary(bookPageGenerated.generatedIllustrationUrl());

        BookInProgress updated = bookInProgressRepository.addPageTo(
                bip.id(),
                new BookPage(
                        bookPageGenerated.context(),
                        result.newUrl(),
                        bip.previousPages().size()
                ));

        return new AiGenerateResult(updated, bookPageGenerated.questions());
    }


    public BookInProgress retrieveById(Actor user, String bipId) {
        BookInProgress found = bookInProgressRepository.retrieveById(bipId);
        if(found == null) {
            throw BookProgressException.notFound("not found book in progress : " + bipId);
        }
        validateOwner(found, user);
        validateNotNull(found, bipId);
        return found;
    }

    private void validateOwner(BookInProgress bookInProgress, Actor user) {
        if(user.role().equals(Role.ADMIN)) {
            return;
        }
        long currentUserId = user.id();
        if(currentUserId != bookInProgress.ownerId()) {
            throw BookProgressException.forbiddenResource();
        }
    }

    private void validateNotNull(BookInProgress bookInProgress, String targetId) {
        if(bookInProgress == null) {
            throw BookProgressException.notFound(targetId);
        }
    }
}
