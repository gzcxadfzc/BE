package com.pkg.openai.api;

import com.pkg.openai.api.request.ChatRequest;
import com.pkg.openai.api.response.ChatResponse;

public interface OpenAiApiPort {

    ChatResponse getResponse(ChatRequest request);

    ChatResponse getResponseById(String responseId);
}
