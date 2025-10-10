package com.pkg.core.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pkg.core.input.GenerateContextQuestionInputDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonableTest {

    @Test
    void getData() throws JsonProcessingException {
        GenerateContextQuestionInputDto inputDto = GenerateContextQuestionInputDto.builder()
                .previousContext("previousContext")
                .personality("personality")
                .mainCharacterName("name")
                .currentContext("currentContext")
                .build();
        String jsonString = """
                {
                "previousContext" : "previousContext",
                "personality" : "personality",
                "mainCharacterName" : "name",
                "currentContext" : "currentContext"
                }
                """;
        Jsonable<GenerateContextQuestionInputDto> inputDtoJsonable = new Jsonable<>(jsonString, GenerateContextQuestionInputDto.class);
    }

    @Test
    void getJsonString() {
    }
}