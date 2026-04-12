package com.pkg.controller.character;

import com.pkg.controller.common.ApiResponse;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterGenerateRequest;
import com.pkg.domain.character.BookCharacterService;
import com.pkg.domain.character.CharacterPollResult;
import com.pkg.domain.member.Actor;
import com.pkg.support.Authenticated;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/character")
public class BookCharacterController {

    private final BookCharacterService bookCharacterService;

    public BookCharacterController(BookCharacterService bookCharacterService) {
        this.bookCharacterService = bookCharacterService;
    }

    @GetMapping("/my")
    public ApiResponse<List<BookCharacterResponse>> retrieveMy(@Authenticated Actor currentUser) {
        List<BookCharacter> bookCharacters = bookCharacterService.retrieveByUser(currentUser);
        List<BookCharacterResponse> responses = bookCharacters.stream()
                .map(BookCharacterResponse::fromBookCharacter)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/board/{id}")
    public ApiResponse<BookCharacterResponse> retrieveById(@PathVariable Long id) {
        BookCharacter bookCharacter = bookCharacterService.retrieveById(id);
        return ApiResponse.success(BookCharacterResponse.fromBookCharacter(bookCharacter));
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<CharacterStatusResponse> requestCreate(
            @Valid @RequestBody BookCharacterCreationRequest request,
            @Authenticated Actor currentUser) {
        BookCharacterGenerateRequest generateRequest = request.toGenerateRequest(currentUser);
        String cipId = bookCharacterService.requestCreate(generateRequest);
        return ApiResponse.success(new CharacterStatusResponse(cipId, "PENDING"));
    }

    @GetMapping("/{cipId}/status")
    public ApiResponse<CharacterStatusResponse> pollStatus(
            @PathVariable String cipId,
            @Authenticated Actor currentUser) {
        CharacterPollResult result = bookCharacterService.pollStatus(currentUser, cipId);
        return ApiResponse.success(new CharacterStatusResponse(cipId, result.status()));
    }

    @PostMapping("/{cipId}/complete")
    public ApiResponse<BookCharacterResponse> completeCharacter(
            @PathVariable String cipId,
            @Authenticated Actor currentUser) {
        BookCharacter character = bookCharacterService.completeCharacter(currentUser, cipId);
        return ApiResponse.success(BookCharacterResponse.fromBookCharacter(character));
    }
}
