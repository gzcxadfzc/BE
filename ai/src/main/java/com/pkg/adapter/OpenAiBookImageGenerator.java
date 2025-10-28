package com.pkg.adapter;

import com.pkg.core.CallStep;
import com.pkg.openai.api.OpenAiApi;
import com.pkg.openai.api.request.ImageRequest;
import com.pkg.openai.api.response.ImageResponse;
import org.springframework.stereotype.Component;

@Component
public class OpenAiBookImageGenerator implements CallStep<BackgroundIllustrationDescription, String> {

    private static final String IMAGE_PROMPT_PREFIX = """
            This should be a full-sized, children's picture book illustration style, boasting pure and vibrant colors.
            - Maintain a flat 2D look with simple shading with no outlines
            - emphasizing the sweet innocence and enchantment of the scene. 
            - no seperated image
            - must depict : ";
            """;
    private final OpenAiApi openAiApi;

    public OpenAiBookImageGenerator(OpenAiApi openAiApi) {
        this.openAiApi = openAiApi;
    }

    @Override
    public String operate(BackgroundIllustrationDescription depictInfo) {
        ImageRequest imageRequest = ImageRequest.dallE2Builder()
                .n(1)
                .size256()
                .prompt(IMAGE_PROMPT_PREFIX + depictInfo.value())
                .build();
        ImageResponse response = openAiApi.getImage(imageRequest);
        return response.data().getFirst().url();
    }
}
