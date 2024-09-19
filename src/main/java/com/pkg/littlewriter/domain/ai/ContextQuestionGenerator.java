package com.pkg.littlewriter.domain.ai;

import com.pkg.littlewriter.domain.ai.input.GenerateContextQuestionInputDto;
import com.pkg.littlewriter.domain.ai.modelBehavior.AiModelBehavior;
import com.pkg.littlewriter.domain.ai.response.GenerateContextQuestionResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContextQuestionGenerator extends Ai<GenerateContextQuestionInputDto, GenerateContextQuestionResponseDto> {
    @Autowired
    protected ContextQuestionGenerator(AiModelBehavior<GenerateContextQuestionInputDto, GenerateContextQuestionResponseDto> generateContextBehavior) {
        super(generateContextBehavior);
    }
}
