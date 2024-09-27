package com.pkg.littlewriter.external.ai;

import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.input.GenerateContextQuestionInputDto;
import com.pkg.littlewriter.external.ai.modelBehavior.AiModelBehavior;
import com.pkg.littlewriter.external.ai.response.GenerateContextQuestionResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiContextQuestionGenerator extends Ai<GenerateContextQuestionInputDto, GenerateContextQuestionResponseDto>{
    @Autowired
    protected AiContextQuestionGenerator(AiModelBehavior<GenerateContextQuestionInputDto, GenerateContextQuestionResponseDto> generateContextBehavior) {
        super(generateContextBehavior);
    }

    @Override
    public GenerateContextQuestionResponseDto getResponseFrom(GenerateContextQuestionInputDto generateContextQuestionInputDto) throws AiException {
        return aiModelBehavior.getResponseFrom(generateContextQuestionInputDto);
    }
}
