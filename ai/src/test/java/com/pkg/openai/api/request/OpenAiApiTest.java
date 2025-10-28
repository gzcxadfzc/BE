package com.pkg.openai.api.request;

import com.pkg.openai.AiFeignConfig;
import com.pkg.openai.api.OpenAiApi;
import com.pkg.openai.api.OpenAiFeignConfig;
import com.pkg.openai.api.common.*;
import com.pkg.openai.api.response.ImageResponse;
import com.pkg.openai.api.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
        OpenAiFeignConfig.class,
        AiFeignConfig.class
})
class OpenAiApiTest {

    @Autowired
    private OpenAiApi openAiApi;

    @Test
    void testDallE2() {
        ImageRequest request = ImageRequest.dallE2Builder()
                .prompt("draw cat")
                .size256()
                .build();
        ImageResponse response = openAiApi.getImage(request);
        System.out.println(response);
    }

    @Test
    void testDallE3() {
        ImageRequest request = ImageRequest.dallE3Builder()
                .prompt("draw cat")
                .standardQuality()
                .size1024()
                .build();
        ImageResponse response = openAiApi.getImage(request);
        System.out.println(response);
    }

    @Test
    void testChat() {
        ChatRequest request = ChatRequest.builder()
                .model(OpenAiModel.GPT_4_O_MINI)
                .addMessage(ChatMessage.ofUser("대한민국의 수도는?"))
                .build();
        ChatResponse response = openAiApi.getChat(request);
        System.out.println(response.getFirstContent());
    }
}