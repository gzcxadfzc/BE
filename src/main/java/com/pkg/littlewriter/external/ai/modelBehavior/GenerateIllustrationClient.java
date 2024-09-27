package com.pkg.littlewriter.external.ai.modelBehavior;

import com.pkg.littlewriter.external.ai.commons.OpenAiModelEnum;
import com.pkg.littlewriter.external.ai.input.GenerateImageInputDto;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenerateIllustrationClient implements AiApiClient<GenerateImageInputDto> {
    private final OpenAiService openAiService;

    @Autowired
    public GenerateIllustrationClient(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public String getResponseFrom(GenerateImageInputDto generateImageInputDto) {
        return generateImageUrl(generateImageInputDto);
    }

    private String generateImageUrl(GenerateImageInputDto aiInput) {
        CreateImageRequest request = CreateImageRequest.builder()
                .model(OpenAiModelEnum.DALL_E_2.getName())
                .quality("standard")
                .size("512x512")
                .prompt(aiInput.getContext())
                .n(1)
                .build();
        return openAiService.createImage(request)
                .getData()
                .get(0)
                .getUrl();
    }
}
