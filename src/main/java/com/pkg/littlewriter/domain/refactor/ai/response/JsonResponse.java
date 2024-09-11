package com.pkg.littlewriter.domain.refactor.ai.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.littlewriter.domain.refactor.ai.AiDataType;

public class JsonResponse<T> extends AiResponse {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String jsonString;
    private final T dto;

    public JsonResponse(String jsonString, Class<T> type) throws JsonProcessingException {
        super(AiDataType.JSON);
        this.jsonString = jsonString;
        this.dto = MAPPER.readValue(this.jsonString, type);
    }

    public T getDto() {
        return this.dto;
    }

    public String getJsonString() {
        return this.jsonString;
    }
}
