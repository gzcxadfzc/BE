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
            BookInProgress target = getBookInProgress(command).markAsPending();
            return bookRepository.saveFrom(target, bip -> {
                validateNotNull(bip.character());
                return Book.completeFromCommand(bip, command);
            });
        });
    }

    private BookCharacter validateNotNull(BookCharacter character) {
        BookCharacter bookCharacter = bookCharacterRepository.retrieveById(character.id());
        if(bookCharacter == null) {
            throw BookProgressException.notFound("bookCharacter:");
        }
        return bookCharacter;
    }

    private BookInProgress getBookInProgress(CompleteBookCommand command) {
        BookInProgress bookInProgress = bookInProgressRepository.retrieveById(command.bookInProgressId());
        if(bookInProgress == null) {
            throw BookProgressException.notFound("not found bookInProgress: " + command.bookInProgressId());
        }
        return bookInProgress;
    }
}
