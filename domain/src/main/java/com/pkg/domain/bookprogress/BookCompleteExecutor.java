package com.pkg.domain.bookprogress;

import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.book.BookRepository;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterRepository;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BookCompleteExecutor {

    private final BookInProgressRepository bookInProgressRepository;
    private final BookCharacterRepository bookCharacterRepository;
    private final BookRepository bookRepository;
    private final ImageRepository imageRepository;
    private final BookInProgressLockExecutor lockExecutor;

    public BookCompleteExecutor(
            BookInProgressRepository bookInProgressRepository,
            BookCharacterRepository bookCharacterRepository,
            BookRepository bookRepository,
            ImageRepository imageRepository,
            BookInProgressLockExecutor lockExecutor
    ) {
        this.bookInProgressRepository = bookInProgressRepository;
        this.bookCharacterRepository = bookCharacterRepository;
        this.bookRepository = bookRepository;
        this.imageRepository = imageRepository;
        this.lockExecutor = lockExecutor;
    }

    public Book completeBook(CompleteBookCommand command) {
        return lockExecutor.saveWithLock(command.bookInProgressId(), () -> {
            BookInProgress updated = getBookInProgress(command)
                    .changeBookPages(this::uploadImagesAndChangeUrls)
                    .markAsCompleted();
            bookInProgressRepository.save(updated);
            return bookRepository.saveFrom(updated, bip -> {
                validateNotNull(bip.character());
                return Book.completeFromCommand(updated, command);
            });
        });
    }

    private List<BookPage> uploadImagesAndChangeUrls(List<BookPage> pages) {
        Map<String, ImageUploadResult> result = imageRepository.copyAllToPermanentStorage(pages.stream().map(BookPage::imageUrl).toList());
        return pages.stream()
                .map(page -> {
                    String newUrl = result.get(page.imageUrl()).newUrl();
                    return new BookPage(page.context(), newUrl, page.pageNumber());
                }).toList();
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
