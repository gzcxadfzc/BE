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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookProgressService {

    private final BookCharacterRepository bookCharacterRepository;
    private final BookInProgressRepository bookInProgressRepository;
    private final BookPageGenerator bookPageGenerator;
    private final ImageRepository imageRepository;
    private final BookInProgressLockExecutor lockExecutor;

    public BookProgressService(
            BookCharacterRepository bookCharacterRepository,
            BookRepository bookRepository,
            BookInProgressRepository bookInProgressRepository,
            BookPageGenerator bookPageGenerator,
            ImageRepository imageRepository,
            BookInProgressLockExecutor lockExecutor
    ) {
        this.bookCharacterRepository = bookCharacterRepository;
        this.bookInProgressRepository = bookInProgressRepository;
        this.bookPageGenerator = bookPageGenerator;
        this.imageRepository = imageRepository;
        this.lockExecutor = lockExecutor;
    }

    public AiGenerateResult initBook(BookInitCommand command) {
        BookCharacter bookCharacter = bookCharacterRepository.retrieveById(command.characterId());
        if(bookCharacter == null) {
            throw BookProgressException.notFound("not found bookCharacter :" + command.characterId());
        }
        BookInProgress bookInProgress = BookInProgress.fromCommand(command, bookCharacter);
        bookInProgressRepository.save(bookInProgress);
        BookPageGenerated bookPageGenerated = bookPageGenerator.generatePageFrom(new BookToProgress(bookInProgress, command.userInput()));
        ImageUploadResult result = imageRepository.uploadTemporary(bookPageGenerated.generatedIllustrationUrl());
        BookInProgress updated = bookInProgressRepository.addPageTo(bookInProgress.id(), new BookPage(bookPageGenerated.context(), result.newUrl(), 0));
        return new AiGenerateResult(updated, bookPageGenerated.questions());
    }

    public AiGenerateResult generateWithAi(CreateOnePageCommand command) {
        return lockExecutor.updateWithLock(command.bipId(), () -> {
            List<String> generatedQuestions = new ArrayList<>();
            BookInProgress bip = bookInProgressRepository.retrieveById(command.bipId());
            BookPageGenerated bookPageGenerated = bookPageGenerator.generatePageFrom(new BookToProgress(bip, command.userInput()));
            ImageUploadResult result = imageRepository.uploadTemporary(bookPageGenerated.generatedIllustrationUrl());
            BookInProgress updated = bip.appendPageFrom(command, bookPageGenerated.context(), result.newUrl());
            BookInProgress appended = bookInProgressRepository.addPageTo(updated.id(), updated.previousPages().getLast());
            return new AiGenerateResult(appended, generatedQuestions);
        });
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
