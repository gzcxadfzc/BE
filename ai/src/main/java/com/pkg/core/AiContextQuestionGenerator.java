package com.pkg.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pkg.core.commons.Jsonable;
import com.pkg.core.exceptions.AiException;
import com.pkg.core.exceptions.AiIllegalFormatResponseException;
import com.pkg.core.input.GenerateContextQuestionInputDto;
import com.pkg.core.response.GenerateContextQuestionResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiContextQuestionGenerator extends Ai<GenerateContextQuestionInputDto, GenerateContextQuestionResponseDto>{
    @Autowired
    protected AiContextQuestionGenerator(AiApiClient<GenerateContextQuestionInputDto> generateContextClient) {
        super(generateContextClient);
    }

    @Override
    public GenerateContextQuestionResponseDto getResponseFrom(GenerateContextQuestionInputDto generateContextQuestionInputDto) throws AiException {
        try {
            String rawResponse = aiApiClient.getResponseFrom(generateContextQuestionInputDto);
            Jsonable<GenerateContextQuestionResponseDto> responseDto = new Jsonable<>(rawResponse, GenerateContextQuestionResponseDto.class);
            return responseDto.getData();
        } catch (JsonProcessingException e) {
            throw new AiIllegalFormatResponseException(e.getMessage());
        }
    }
}
