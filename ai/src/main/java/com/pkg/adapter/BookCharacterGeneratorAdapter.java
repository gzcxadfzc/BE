package com.pkg.adapter;

import com.pkg.domain.ai.BookCharacterGenerator;
import com.pkg.domain.character.BookCharacterCreateCommand;
import org.springframework.stereotype.Component;

@Component
public class BookCharacterGeneratorAdapter implements BookCharacterGenerator {

    private final OpenAIBookCharacterGenerator generator;

    public BookCharacterGeneratorAdapter(OpenAIBookCharacterGenerator generator) {
        this.generator = generator;
    }

    @Override
    public String generateImageFrom(BookCharacterCreateCommand bookCharacterCreateCommand) {
        String appearance = "appearance : " + bookCharacterCreateCommand.appearanceKeywords();
        String description = "description: " + bookCharacterCreateCommand.description();
        return generator.generateImage(appearance + description);
    }
}
