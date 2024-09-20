package com.pkg.littlewriter.domain.ai.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsonable<T>{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String jsonString;
    private final T data;

    public Jsonable(String jsonString, Class<T> type) throws JsonProcessingException {
        this.data = MAPPER.readValue(jsonString, type);
        this.jsonString = jsonString;
    }

    public Jsonable(T data) throws JsonProcessingException {
        this.jsonString = MAPPER.writeValueAsString(data);
        this.data = data;
    }

    public T getData() {
        return this.data;
    }

    public String getJsonString() {
        return this.jsonString;
    }
}
