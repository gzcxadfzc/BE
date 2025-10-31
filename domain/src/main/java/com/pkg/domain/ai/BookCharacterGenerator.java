package com.pkg.domain.ai;

import com.pkg.domain.character.BookCharacterGenerateRequest;

public interface BookCharacterGenerator {

    String generateImageFrom(BookCharacterGenerateRequest request);
}
