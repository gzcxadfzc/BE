package com.pkg.littlewriter.domain.ai;

import com.pkg.littlewriter.domain.ai.input.AiJsonInput;
import com.pkg.littlewriter.domain.ai.input.AiTextInput;
import com.pkg.littlewriter.domain.ai.modelBehavior.AiModelBehavior;
import com.pkg.littlewriter.domain.ai.response.AiJsonResponse;
import com.pkg.littlewriter.domain.ai.response.ContextAndQuestionDto;
import com.pkg.littlewriter.domain.ai.commons.Jsonable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContextQuestionGenerator extends Ai<AiJsonInput<?>, AiJsonResponse<ContextAndQuestionDto>> {
    @Autowired
    protected ContextQuestionGenerator(AiModelBehavior<AiJsonInput<?>, AiJsonResponse<ContextAndQuestionDto>> generateContextBehavior) {
        super(generateContextBehavior);
    }
}
