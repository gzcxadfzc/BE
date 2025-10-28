package com.pkg.domain.ai;

import com.pkg.domain.character.BookCharacterCreateCommand;

public interface BookCharacterGenerator {

    String generateImageFrom(BookCharacterCreateCommand bookCharacterCreateCommand);
}
