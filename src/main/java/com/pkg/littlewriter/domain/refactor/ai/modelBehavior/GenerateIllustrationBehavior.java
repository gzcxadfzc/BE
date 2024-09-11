package com.pkg.littlewriter.domain.refactor.ai.modelBehavior;

import com.pkg.littlewriter.domain.generativeAi.openAiModels.OpenAiModelEnum;
import com.pkg.littlewriter.domain.refactor.ai.response.AiImageResponse;
import com.pkg.littlewriter.domain.refactor.ai.input.AiTextInput;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenerateIllustrationBehavior implements AiModelBehavior<AiTextInput, AiImageResponse> {
    private final OpenAiService openAiService;

    @Autowired
    public GenerateIllustrationBehavior(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public AiImageResponse getResponseFrom(AiTextInput aiInput) {
        CreateImageRequest request = CreateImageRequest.builder()
                .model(OpenAiModelEnum.DALL_E_2.getName())
                .quality("standard")
                .size("512x512")
                .prompt(aiInput.getInputText())
                .n(1)
                .build();
        String resultImageUrl = openAiService.createImage(request)
                .getData()
                .get(0)
                .getUrl();
        return new AiImageResponse(resultImageUrl);
    }
}
