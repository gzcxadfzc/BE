package com.pkg.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.core.CallStep;
import com.pkg.openai.api.OpenAiApi;
import com.pkg.openai.api.common.JsonSchema;
import com.pkg.openai.api.common.OpenAiModel;
import com.pkg.openai.api.common.TextFormat;
import com.pkg.openai.api.exception.OpenAiApiException;
import com.pkg.openai.api.request.ChatMessage;
import com.pkg.openai.api.request.ChatRequest;
import com.pkg.openai.api.response.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class OpenAiContextQuestionGenerator implements CallStep<BookProgressInfo, ContextQuestion> {

    private static final ChatMessage SYSTEM_PROMPT = ChatMessage.ofDeveloper("""
            you're a helpful assistant who helps writing a fairy tale
            if sentences are given
            - <context> should enrich sentences more naturally in fairy tale storybook style, using ~해요 체
            - create 3-short <question> about current sentences
                <questions> can based on character's personality
                <questions> must related to what will happen next
                <questions> must seperated by linebreak. no index.
                <questions> must be korean
            """);

    private static final JsonSchema RESPONSE_SCHEMA = JsonSchema.builder()
            .name("contextQuestion")
            .schema("""
                    {
                        "type": "object",
                        "properties" : {
                            "context" : { "type" : "string" },
                            "questions" : {
                                "type" : "array" ,
                                "items" : {"type" : "string"}
                            }
                        },
                        "required" : ["context", "questions"],
                        "additionalProperties" : false
                    }""")
            .strict(true)
            .build();

    private final OpenAiApi openAiApi;
    private final ObjectMapper mapper;

    public OpenAiContextQuestionGenerator(OpenAiApi openAiApi, ObjectMapper mapper) {
        this.openAiApi = openAiApi;
        this.mapper = mapper;
    }

    @Override
    public ContextQuestion operate(BookProgressInfo info) {
        try{

            String userInput = mapper.writeValueAsString(info);
            ChatRequest request = ChatRequest.builder()
                    .model(OpenAiModel.GPT_4_O_MINI)
                    .addMessage(SYSTEM_PROMPT)
                    .addMessage(ChatMessage.ofUser(userInput))
                    .text(TextFormat.from(RESPONSE_SCHEMA))
                    .build();

            ChatResponse response = openAiApi.getChat(request);
            String value = response.getFirstContent();
            return mapper.readValue(value, ContextQuestion.class);
        } catch (JsonProcessingException e) {
            throw new OpenAiApiException(500, e.getMessage());
        }
    }
}
