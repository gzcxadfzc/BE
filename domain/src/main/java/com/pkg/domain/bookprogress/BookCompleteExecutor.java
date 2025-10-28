package com.pkg.domain.bookprogress;

import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.book.BookRepository;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterRepository;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import com.pkg.domain.member.Actor;
import com.pkg.domain.member.Role;
import com.pkg.domain.uitl.UuidGen;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BookCompleteExecutor {

    private final BookInProgressRepository bookInProgressRepository;
    private final BookCharacterRepository bookCharacterRepository;
    private final BookRepository bookRepository;
    private final ImageRepository imageRepository;

    public BookCompleteExecutor(BookInProgressRepository bookInProgressRepository, BookCharacterRepository bookCharacterRepository, BookRepository bookRepository, ImageRepository imageRepository) {
        this.bookInProgressRepository = bookInProgressRepository;
        this.bookCharacterRepository = bookCharacterRepository;
        this.bookRepository = bookRepository;
        this.imageRepository = imageRepository;
    }

    public Book completeBook(CompleteBookCommand command) {
        BookInProgress bookInProgress = getBookInProgress(command);
        Map<String, ImageUploadResult> result = imageRepository.copyAllToPermanentStorage(
                bookInProgress.previousPages().stream()
                        .map(BookPage::imageUrl)
                        .toList());
        List<BookPage> newBookPages = bookInProgress.previousPages()
                .stream()
                .map(page -> {
                    String newUrl = result.get(page.imageUrl()).newUrl();
                    return new BookPage(page.context(), newUrl, page.pageNumber());
                }).toList();
        return bookRepository.saveFrom(bookInProgress, bip -> {
            validateOwner(bip, command.actor());
            validateCharacter(bip);
            return Book.builder()
                    .title(command.title())
                    .id(UuidGen.compact())
                    .memberId(command.actor().id())
                    .character(bip.character())
                    .bookPages(newBookPages)
                    .author(command.author())
                    .build();
        });
    }

    private BookCharacter validateCharacter(BookInProgress bookInProgress) {
        BookCharacter bookCharacter = bookCharacterRepository.retrieveById(bookInProgress.character().id());
        if(bookCharacter == null) {
            throw BookProgressException.notFound("bookCharacter:" + bookInProgress.character().id());
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

    private void validateOwner(BookInProgress bookInProgress, Actor user) {
        if(user.role().equals(Role.ADMIN)) {
            return;
        }
        long currentUserId = user.id();
        if(currentUserId != bookInProgress.ownerId()) {
            throw BookProgressException.forbiddenResource();
        }
    }
}
