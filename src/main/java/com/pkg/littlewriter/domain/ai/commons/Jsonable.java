package com.pkg.littlewriter.domain.ai.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsonable<T>{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String jsonString;
    private final T data;

    public Jsonable(String jsonString, Class<T> type) throws JsonProcessingException {
        this.jsonString = jsonString;
        this.data = MAPPER.readValue(this.jsonString, type);
    }

    public T getData() {
        return this.data;
    }

    public String getJsonString() {
        return this.jsonString;
    }
}
