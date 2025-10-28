package com.pkg.controller.character;

import com.pkg.controller.common.ApiResponse;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/character")
public class BookCharacterController {

    private final BookCharacterService bookCharacterService;

    public BookCharacterController(BookCharacterService bookCharacterService) {
        this.bookCharacterService = bookCharacterService;
    }

    @GetMapping("/{id}")
    public ApiResponse<BookCharacterResponse> retrieveById(@PathVariable Long id) {
        BookCharacter bookCharacter = bookCharacterService.retrieveById(id);
        return ApiResponse.success(BookCharacterResponse.fromBookCharacter(bookCharacter));
    }

//    @PostMapping
//    public ApiResponse<BookCharacterResponse> createBookCharacter(@RequestBody BookCharacterCreationRequest) {
//
//    }

}
