package com.pkg.littlewriter.service;

import com.pkg.littlewriter.persistence.BookEntity;
import com.pkg.littlewriter.persistence.BookPageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookService {
    @Autowired
    private BookPageRepository bookRepository;

    public BookEntity createEmptyBook(BookEntity bookEntity) {
        bookRepository.save(bookEntity);
        return bookRepository.findById(bookEntity.getId()).orElseThrow();
    }

    public Page<BookEntity> getAllByUserId(Long userId, Pageable pageable) {
        return  bookRepository.findAllByUserId(userId, pageable);
    }

    public BookEntity getById(String bookId) {
        return bookRepository.findById(bookId).orElseThrow();
    }
}
