package com.pkg.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.core.CallStep;
import com.pkg.openai.api.OpenAiApi;
import com.pkg.openai.api.common.OpenAiModel;
import com.pkg.openai.api.exception.OpenAiInternalException;
import com.pkg.openai.api.request.ChatMessage;
import com.pkg.openai.api.request.ChatRequest;
import com.pkg.openai.api.response.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class OpenAiContextDepictGenerator implements CallStep<BookProgressInfo, BackgroundIllustrationDescription> {
    private static final List<String> DEPICT_OPTIONS = List.of(
            "[whole context of appearanceKeyword] is [what main character is doing], in [background], [lights]",
            "[other character] is [what they're doing], in [background], [lights]",
            "[object] is in [background], [lights]",
            "[background], [lights]"
    );
    private static final String PROMPT_DEVELOPER = """
            you're a helpful assistant that depict details to generate image.
                 using given json, depict "currentContext" scene.
            - must under 500 letters
            """;
    private static final Random RANDOM = new Random();

    private final ObjectMapper mapper;
    private final OpenAiApi api;

    public OpenAiContextDepictGenerator(ObjectMapper mapper, OpenAiApi api) {
        this.mapper = mapper;
        this.api = api;
    }

    @Override
    public BackgroundIllustrationDescription operate(BookProgressInfo info) {
        try {
            String input = mapper.writeValueAsString(info);
            ChatRequest request = ChatRequest.builder()
                    .model(OpenAiModel.GPT_4_O_MINI)
                    .addMessage(getRandomDeveloperPrompt())
                    .addMessage(ChatMessage.ofUser(input))
                    .build();
            ChatResponse response = api.getChat(request);
            return new BackgroundIllustrationDescription(response.getFirstContent());
        } catch (JsonProcessingException e) {
            throw new OpenAiInternalException("cannot convert to String from " + info);
        }
    }

    private ChatMessage getRandomDeveloperPrompt() {
        String randomOption = DEPICT_OPTIONS.get(RANDOM.nextInt(4));
        return ChatMessage.ofDeveloper(PROMPT_DEVELOPER + randomOption);
    }
}
