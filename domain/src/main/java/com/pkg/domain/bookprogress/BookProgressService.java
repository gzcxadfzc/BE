package com.pkg.domain.bookprogress;

import com.pkg.domain.ai.CreateOnePageCommand;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterRepository;
import com.pkg.domain.member.Actor;
import com.pkg.domain.member.Role;
import org.springframework.stereotype.Service;

@Service
public class BookProgressService {

    private final BookCharacterRepository bookCharacterRepository;
    private final BookInProgressRepository bookInProgressRepository;
    private final BookInProgressLockExecutor lockExecutor;
    private final BookPageQueuePublisher queuePublisher;
    private final BookPageResultRepository bookPageResultRepository;

    public BookProgressService(
            BookCharacterRepository bookCharacterRepository,
            BookInProgressRepository bookInProgressRepository,
            BookInProgressLockExecutor lockExecutor,
            BookPageQueuePublisher queuePublisher,
            BookPageResultRepository bookPageResultRepository
    ) {
        this.bookCharacterRepository = bookCharacterRepository;
        this.bookInProgressRepository = bookInProgressRepository;
        this.lockExecutor = lockExecutor;
        this.queuePublisher = queuePublisher;
        this.bookPageResultRepository = bookPageResultRepository;
    }

    public BookPageAccepted initBook(BookInitCommand command) {
        BookCharacter bookCharacter = bookCharacterRepository.retrieveById(command.characterId());
        if (bookCharacter == null) {
            throw BookProgressException.notFound("not found bookCharacter :" + command.characterId());
        }
        BookInProgress bip = BookInProgress.fromCommand(command, bookCharacter);
        bookInProgressRepository.save(bip);
        queuePublisher.publish(new BookPageQueueMessage(
                bip.id(), 0, command.userInput(),
                bookCharacter.name(), bookCharacter.description(), command.background()));
        return new BookPageAccepted(bip.id());
    }

    public BookPageAccepted generateWithAi(CreateOnePageCommand command) {
        return lockExecutor.updateWithLock(command.bipId(), () -> {
            BookInProgress bip = bookInProgressRepository.retrieveById(command.bipId());
            if (bip == null) {
                throw BookProgressException.notFound(command.bipId());
            }
            if (bip.status() == BookInProgress.Status.PENDING) {
                throw BookProgressException.alreadyPending(command.bipId());
            }
            validateOwner(bip, command.currentUser());
            int nextPageIndex = bip.previousPages().size();
            bookInProgressRepository.save(bip.markAsPending());
            queuePublisher.publish(new BookPageQueueMessage(
                    bip.id(), nextPageIndex, command.userInput(),
                    bip.character().name(), bip.character().description(), bip.backgroundInfo()));
            return new BookPageAccepted(bip.id());
        });
    }

    public BookInProgress retrieveById(Actor user, String bipId) {
        BookInProgress found = bookInProgressRepository.retrieveById(bipId);
        if (found == null) {
            throw BookProgressException.notFound("not found book in progress : " + bipId);
        }
        validateOwner(found, user);
        return found;
    }

    public BookPagePollResult pollPageResult(Actor user, String bipId) {
        BookInProgress bip = retrieveById(user, bipId);
        return bookPageResultRepository.find(bipId)
                .map(page -> {
                    BookInProgress updated = bip.addBookPage(
                            new BookPage(page.context(), page.imageUrl(), page.pageIndex()));
                    bookInProgressRepository.save(updated);
                    bookPageResultRepository.delete(bipId);
                    return BookPagePollResult.completed(page);
                })
                .orElseGet(BookPagePollResult::pending);
    }

    private void validateOwner(BookInProgress bookInProgress, Actor user) {
        if (user.role().equals(Role.ADMIN)) {
            return;
        }
        if (!user.id().equals(bookInProgress.ownerId())) {
            throw BookProgressException.forbiddenResource();
        }
    }
}
