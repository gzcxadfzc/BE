package com.pkg.littlewriter.external.ai.modelBehavior;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pkg.littlewriter.external.ai.commons.OpenAiModelEnum;
import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.exceptions.AiIllegalFormatResponseException;
import com.pkg.littlewriter.external.ai.input.GenerateContextQuestionInputDto;
import com.pkg.littlewriter.external.ai.response.GenerateContextQuestionResponseDto;
import com.pkg.littlewriter.external.ai.commons.Jsonable;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateContextBehavior implements AiModelBehavior<GenerateContextQuestionInputDto, GenerateContextQuestionResponseDto> {
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
    public GenerateContextQuestionResponseDto getResponseFrom(GenerateContextQuestionInputDto aiInput) throws AiException {
        try {
            Jsonable<GenerateContextQuestionInputDto> inputJsonable = new Jsonable<>(aiInput);
            ChatMessage response = getChatMessage(inputJsonable);
            Jsonable<GenerateContextQuestionResponseDto> responseJsonable = new Jsonable<>(response.getContent(), GenerateContextQuestionResponseDto.class);
            return responseJsonable.getData();
        } catch (JsonProcessingException e) {
            throw new AiIllegalFormatResponseException(e.getMessage());
        }
    }

    private ChatMessage getChatMessage(Jsonable<GenerateContextQuestionInputDto> inputJsonable) {
        ChatMessage fairyTaleInfo = new ChatMessage("user", inputJsonable.getJsonString());
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(OpenAiModelEnum.GPT_4_TURBO_PREVIEW.getName())
                .messages(List.of(SYSTEM_MESSAGE, fairyTaleInfo))
                .temperature(0.5)
                .maxTokens(500)
                .build();
        ChatCompletionChoice choice =  openAiService.createChatCompletion(request)
                .getChoices()
                .get(0);
        return choice.getMessage();
    }
}
