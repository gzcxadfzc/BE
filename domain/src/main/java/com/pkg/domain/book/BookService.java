package com.pkg.domain.book;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookThumbnail> retrieveBookThumbnailsByMemberId(String memberId) {
        List<BookThumbnail> thumbnails = bookRepository.retrieveThumbnailsByMemberId(memberId);
        if(thumbnails == null) {
            throw BookException.emptyBookException(memberId);
        }
        return bookRepository.retrieveThumbnailsByMemberId(memberId);
    }

    public Book retrieveByBookId(String bookId) {
        Book book = bookRepository.retrieveById(bookId);
        if(book == null) {
            throw BookException.bookNotFoundException(bookId);
        }
        return bookRepository.retrieveById(bookId);
    }
}
