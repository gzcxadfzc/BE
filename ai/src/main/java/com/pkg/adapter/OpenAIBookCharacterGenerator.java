package com.pkg.adapter;

import com.pkg.core.CallStep;
import com.pkg.domain.character.BookCharacterGenerateRequest;
import com.pkg.openai.api.OpenAiApi;
import com.pkg.openai.api.request.ImageRequest;
import com.pkg.openai.api.response.ImageResponse;
import org.springframework.stereotype.Component;

@Component
public class OpenAIBookCharacterGenerator implements CallStep<BookCharacterGenerateRequest, String> {

    private static final String IMAGE_PROMPT_PREFIX = """
            This should be a full-sized, children's picture book illustration style, boasting pure and vibrant colors.
            - Maintain a flat 2D look with simple shading with no outlines
            - emphasizing the sweet innocence and enchantment of the scene. 
            - no seperated image
            - create a BookCharacter using information given."
            """;

    private final OpenAiApi api;

    public OpenAIBookCharacterGenerator(OpenAiApi api) {
        this.api = api;
    }

    @Override
    public String operate(BookCharacterGenerateRequest generateRequest) {

        ImageRequest request = ImageRequest.dallE2Builder()
                .n(1)
                .prompt(
                        IMAGE_PROMPT_PREFIX
                        + "description:" + generateRequest.description()
                        + ",appearance:" + generateRequest.appearanceKeywords())
                .size256()
                .build();

        ImageResponse response = api.getImage(request);
        return response.data().getFirst().url();
    }
}
