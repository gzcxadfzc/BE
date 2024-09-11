package com.pkg.littlewriter.domain.refactor.ai.modelBehavior;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pkg.littlewriter.domain.generativeAi.openAiModels.OpenAiModelEnum;
import com.pkg.littlewriter.domain.refactor.ai.input.AiTextInput;
import com.pkg.littlewriter.domain.refactor.ai.response.ContextAndQuestionDto;
import com.pkg.littlewriter.domain.refactor.ai.response.JsonResponse;
import com.pkg.littlewriter.domain.refactor.ai.exceptions.AiException;
import com.pkg.littlewriter.domain.refactor.ai.exceptions.AiIllegalFormatResponseException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateContextBehavior implements AiModelBehavior<AiTextInput, JsonResponse<ContextAndQuestionDto>>{
    private final OpenAiService openAiService;

    private static final ChatMessage SYSTEM_MESSAGE = new ChatMessage("system",
            """
                      you're a helpful assistant who helps writing a fairy tale
                        if sentences are given
                        first, enrich sentences more naturally in fairy tale storybook style, using ~해요 체
                        second, create 3-short question about current sentences
                          questions can based on character's personality
                          questions must related to what will happen next
                          questions must seperated by linebreak. no index.
                          questions must be korean
                        answer format must be json
                        {
                          "refinedText" : "",
                          "questions" : []
                        }
                    """
    );

    @Autowired
    public GenerateContextBehavior(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public JsonResponse<ContextAndQuestionDto> getResponseFrom(AiTextInput aiInput) throws AiException {
        ChatMessage fairyTaleInfo = new ChatMessage("user", aiInput.getInputText());
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(OpenAiModelEnum.GPT_4_TURBO_PREVIEW.getName())
                .messages(List.of(SYSTEM_MESSAGE, fairyTaleInfo))
                .temperature(0.5)
                .maxTokens(500)
                .build();
        ChatMessage response = openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage();
        try {
            return new JsonResponse<>(response.getContent(), ContextAndQuestionDto.class);
        } catch (JsonProcessingException e) {
            throw new AiIllegalFormatResponseException(e.getMessage() + " ai generated answer with wrong format");
        }
    }
}
