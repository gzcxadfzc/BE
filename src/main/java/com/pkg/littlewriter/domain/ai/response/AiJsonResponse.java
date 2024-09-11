package com.pkg.littlewriter.domain.ai.response;

import com.pkg.littlewriter.domain.ai.commons.AiDataType;
import com.pkg.littlewriter.domain.ai.commons.Jsonable;

public class AiJsonResponse<T> extends AiResponse {
    private final Jsonable<T> jsonable;

    public AiJsonResponse(Jsonable<T> jsonable) {
        super(AiDataType.JSON);
        this.jsonable = jsonable;
    }

    public String getJsonString() {
        return jsonable.getJsonString();
    }

    public Object getData() {
        return jsonable.getData();
    }
}
