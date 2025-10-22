package com.pkg.controller.book;

import com.pkg.controller.common.ApiResponse;
import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookService;
import com.pkg.domain.book.BookThumbnail;
import com.pkg.domain.member.Actor;
import com.pkg.support.Authenticated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/book")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/board/{bookId}")
    ApiResponse<BookResponse> getBookById(@PathVariable String bookId) {
        Book book = bookService.retrieveByBookId(bookId);
        return ApiResponse.success(BookResponse.from(book));
    }

    @GetMapping("/my")
    ApiResponse<BookThumbnailResponse> getBookById(@Authenticated Actor currentUser) {
        List<BookThumbnail> thumbnails = bookService.retrieveBookThumbnailsByOwner(currentUser);
        return ApiResponse.success(BookThumbnailResponse.from(thumbnails));
    }
}
