package com.pkg.openai.api;

import com.pkg.openai.api.exception.OpenAiApiException;
import com.pkg.openai.api.request.ChatRequest;
import com.pkg.openai.api.response.ChatResponse;
import feign.RetryableException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class OpenAiApiAdapter implements OpenAiApiPort {

    private final OpenAiApi api;

    public OpenAiApiAdapter(OpenAiApi api) {
        this.api = api;
    }

    @Override
    public ChatResponse getResponse(ChatRequest request) {
        return handleRetryable(() -> api.getChat(request));
    }

    @Override
    public ChatResponse getResponseById(String responseId) {
        return handleRetryable(() -> api.getChat(responseId));
    }

    private ChatResponse handleRetryable(Supplier<ChatResponse> responseSupplier) {
        try{
            return responseSupplier.get();
        } catch (RetryableException e) {
            throw new OpenAiApiException(500, "retryable exception occurs");
        }
    }
}
