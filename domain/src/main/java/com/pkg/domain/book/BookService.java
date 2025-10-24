package com.pkg.domain.book;

import com.pkg.domain.bookprogress.CompleteBookRequest;
import com.pkg.domain.common.PageResult;
import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookThumbnail> retrieveBookThumbnailsByOwner(Actor currentUser) {
        return bookRepository.retrieveThumbnailsByUser(currentUser);
    }

    public PageResult<BookThumbnail> retrieveBookThumbnails(BookRetrieveQuery query) {
        return bookRepository.retrieveThumbnails(query);
    }

    public Book retrieveByBookId(String bookId) {
        Book book = bookRepository.retrieveById(bookId);
        if(book == null) {
            throw BookException.notFound(bookId);
        }
        return bookRepository.retrieveById(bookId);
    }

    public Book completeBook(CompleteBookRequest request) {
        Book book = Book.builder()
                .title(request.title())
                .id(request.bookInProgress().id())
                .character(request.bookInProgress().character())
                .bookPages(request.bookInProgress().previousPages())
                .author(request.author())
                .build();
        validateOwner(request);
        return bookRepository.save(book);
    }

    private void validateOwner(CompleteBookRequest request) {
        if(!Objects.equals(request.bookInProgress().id(), request.memberId())) {
            throw BookException.notAuthorizedBookCreationFrom(request.bookInProgress());
        }
    }
}
