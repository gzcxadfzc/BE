package com.pkg.domain.bookprogress;

import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookRepository;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterRepository;
import org.springframework.stereotype.Component;

@Component
public class BookCompleteExecutor {

    private final BookInProgressRepository bookInProgressRepository;
    private final BookCharacterRepository bookCharacterRepository;
    private final BookRepository bookRepository;
    private final BookInProgressLockExecutor lockExecutor;

    public BookCompleteExecutor(
            BookInProgressRepository bookInProgressRepository,
            BookCharacterRepository bookCharacterRepository,
            BookRepository bookRepository,
            BookInProgressLockExecutor lockExecutor
    ) {
        this.bookInProgressRepository = bookInProgressRepository;
        this.bookCharacterRepository = bookCharacterRepository;
        this.bookRepository = bookRepository;
        this.lockExecutor = lockExecutor;
    }

    public Book completeBook(CompleteBookCommand command) {
        return lockExecutor.saveWithLock(command.bookInProgressId(), () -> {
            BookInProgress target = getBookInProgress(command);
            if (target.status() == BookInProgress.Status.PENDING) {
                throw BookProgressException.alreadyPending(command.bookInProgressId());
            }
            BookCharacter character = bookCharacterRepository.retrieveById(target.character().id());
            if (character == null) {
                throw BookProgressException.notFound("bookCharacter:");
            }
            return bookRepository.saveFrom(target, character,
                    bip -> Book.completeFromCommand(bip, command));
        });
    }

    private BookInProgress getBookInProgress(CompleteBookCommand command) {
        BookInProgress bookInProgress = bookInProgressRepository.retrieveById(command.bookInProgressId());
        if(bookInProgress == null) {
            throw BookProgressException.notFound("not found bookInProgress: " + command.bookInProgressId());
        }
        return bookInProgress;
    }
}
