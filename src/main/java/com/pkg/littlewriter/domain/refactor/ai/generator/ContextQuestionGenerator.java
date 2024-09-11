package com.pkg.littlewriter.domain.refactor.ai.generator;

import com.pkg.littlewriter.domain.refactor.ai.input.AiTextInput;
import com.pkg.littlewriter.domain.refactor.ai.modelBehavior.AiModelBehavior;
import com.pkg.littlewriter.domain.refactor.ai.response.ContextAndQuestionDto;
import com.pkg.littlewriter.domain.refactor.ai.response.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContextQuestionGenerator extends Ai<AiTextInput, JsonResponse<ContextAndQuestionDto>> {
    @Autowired
    protected ContextQuestionGenerator(AiModelBehavior<AiTextInput, JsonResponse<ContextAndQuestionDto>> generateContextBehavior) {
        super(generateContextBehavior);
    }
}
