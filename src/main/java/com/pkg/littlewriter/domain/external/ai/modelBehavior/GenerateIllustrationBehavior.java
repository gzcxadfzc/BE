package com.pkg.littlewriter.domain.external.ai.modelBehavior;

import com.pkg.littlewriter.domain.external.ai.commons.OpenAiModelEnum;
import com.pkg.littlewriter.domain.external.ai.input.GenerateImageInputDto;
import com.pkg.littlewriter.domain.external.ai.response.GenerateImageResponseDto;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenerateIllustrationBehavior implements AiModelBehavior<GenerateImageInputDto, GenerateImageResponseDto> {
    private final OpenAiService openAiService;

    @Autowired
    public GenerateIllustrationBehavior(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public GenerateImageResponseDto getResponseFrom(GenerateImageInputDto generateImageInputDto) {
        String url = generateImageUrl(generateImageInputDto);
        return new GenerateImageResponseDto(url);
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
