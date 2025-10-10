package com.pkg.domain.book;

import org.springframework.stereotype.Service;

@Service
public class BookProgressService {

    public BookInProgressRepository bookInProgressRepository;
    public BookPageGenerator bookPageGenerator;
    public BookRepository bookRepository;

    public BookProgressService(
            BookInProgressRepository bookInProgressRepository,
            BookPageGenerator bookPageGenerator,
            BookRepository bookRepository
    ) {
        this.bookInProgressRepository = bookInProgressRepository;
        this.bookPageGenerator = bookPageGenerator;
        this.bookRepository = bookRepository;
    }

    public BookInProgress createOnePage(CreateOnePageRequest request) {
        BookInProgress bookInProgress = retrieveBook(request.bookInProgressId());
        BookToProgress bookToProgress = new BookToProgress(bookInProgress, request.userInput());
        BookPage generatedPage = bookPageGenerator.createOnePage(bookToProgress);
        bookInProgressRepository.addPage(request.bookInProgressId(), generatedPage);
        return bookInProgress.addBookPage(generatedPage);
    }

    public Book finishBook(BookFinishRequest request) {
        BookInProgress bookInProgress = retrieveBook(request.bookInProgressId());
        Book book = Book.builder()
                .id("")
                .bookPages(bookInProgress.previousPages())
                .author(request.author())
                .memberId(request.memberId())
                .character(bookInProgress.character())
                .title(request.title())
                .build();
        bookRepository.save(book);
        return book;
    }

    public BookInProgress initBookInProgress(BookInitRequest request) {
        BookInProgress bookInProgress = bookPageGenerator.initBook(request);
        bookInProgressRepository.save(bookInProgress);
        return bookInProgress;
    }

    private BookInProgress retrieveBook(String request) {
        BookInProgress bookInProgress = bookInProgressRepository.retrieveById(request);
        if (bookInProgress == null) {
            throw BookProgressException.bookInProgressNotFoundException(request);
        }
        return bookInProgress;
    }
}
