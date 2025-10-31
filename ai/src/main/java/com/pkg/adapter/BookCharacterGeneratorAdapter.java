package com.pkg.adapter;

import com.pkg.domain.ai.BookCharacterGenerator;
import com.pkg.domain.character.BookCharacterGenerateRequest;
import org.springframework.stereotype.Component;

@Component
public class BookCharacterGeneratorAdapter implements BookCharacterGenerator {

    private final OpenAIBookCharacterGenerator generator;

    public BookCharacterGeneratorAdapter(OpenAIBookCharacterGenerator generator) {
        this.generator = generator;
    }

    @Override
    public String generateImageFrom(BookCharacterGenerateRequest request) {
        return generator.operate(request);
    }
}
