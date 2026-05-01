package com.pkg.domain.book;

import com.pkg.domain.common.PageResult;
import com.pkg.domain.common.SliceResult;
import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public SliceResult<BookThumbnail> retrieveBookThumbnailsSlice(BookRetrieveQuery query) {
        return bookRepository.retrieveThumbnailsSlice(query);
    }

    public Book retrieveByBookId(String bookId) {
        Book book = bookRepository.retrieveById(bookId);
        if(book == null) {
            throw BookException.notFound(bookId);
        }
        return book;
    }
}
