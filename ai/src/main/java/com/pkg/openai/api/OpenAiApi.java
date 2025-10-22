package com.pkg.openai.api;

import com.pkg.openai.api.request.ChatRequest;
import com.pkg.openai.api.request.ImageRequest;
import com.pkg.openai.api.response.ImageResponse;
import com.pkg.openai.api.response.ChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "${ai.open-ai.api.username}",
        url = "${ai.open-ai.api.url}",
        configuration = OpenAiFeignConfig.class
)
public interface OpenAiApi {

    @PostMapping("/responses")
    ChatResponse getChat(@RequestBody ChatRequest chatCompletionRequest);

    @GetMapping({"/responses/{responseId}"})
    ChatResponse getChat(@PathVariable("responseId") String responseId);

    @PostMapping("/images/generations")
    ImageResponse getImage(@RequestBody ImageRequest imageRequest);
}