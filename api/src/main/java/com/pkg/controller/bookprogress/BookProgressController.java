package com.pkg.controller.bookprogress;

import com.pkg.controller.book.BookResponse;
import com.pkg.controller.common.ApiResponse;
import com.pkg.domain.ai.CreateOnePageCommand;
import com.pkg.domain.book.Book;
import com.pkg.domain.bookprogress.*;
import com.pkg.domain.member.Actor;
import com.pkg.support.Authenticated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/book/progress")
public class BookProgressController {

    private final BookProgressService bookProgressService;
    private final BookCompleteExecutor bookCreator;

    public BookProgressController(BookProgressService bookProgressService, BookCompleteExecutor bookCreator) {
        this.bookProgressService = bookProgressService;
        this.bookCreator = bookCreator;
    }

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<BookPageAcceptedResponse>> initBook(
            @Authenticated Actor currentUser,
            @RequestBody BookInitRequest request) {
        BookInitCommand command = new BookInitCommand(
                request.characterId(),
                request.backgroundInfo(),
                currentUser,
                request.userInput()
        );
        BookPageAccepted accepted = bookProgressService.initBook(command);
        return ResponseEntity.accepted().body(ApiResponse.success(BookPageAcceptedResponse.from(accepted)));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<BookResponse> completeBook(
            @Authenticated Actor currentUser,
            @PathVariable String id,
            @RequestBody BookCompleteRequest request) {
        CompleteBookCommand command = new CompleteBookCommand(
                currentUser,
                id,
                request.title(),
                request.author()
        );
        Book book = bookCreator.completeBook(command);
        return ApiResponse.success(BookResponse.from(book));
    }

    @PostMapping("/{id}")
    public ResponseEntity<ApiResponse<BookPageAcceptedResponse>> generatePage(
            @Authenticated Actor currentUser,
            @RequestBody BookProgressRequest request,
            @PathVariable String id
    ) {
        CreateOnePageCommand command = new CreateOnePageCommand(
                id,
                request.userInput(),
                currentUser
        );
        BookPageAccepted accepted = bookProgressService.generateWithAi(command);
        return ResponseEntity.accepted().body(ApiResponse.success(BookPageAcceptedResponse.from(accepted)));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookInProgressResponse> retrieveBookInProgress(
            @Authenticated Actor currentUser,
            @PathVariable String id
    ) {
        BookInProgress bookInProgress = bookProgressService.retrieveById(currentUser, id);
        return ApiResponse.success(BookInProgressResponse.from(bookInProgress));
    }
}
