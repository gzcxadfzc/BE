package com.pkg.littlewriter.domain.ai.input;

import com.pkg.littlewriter.domain.ai.commons.AiDataType;
import com.pkg.littlewriter.domain.ai.commons.Jsonable;

public class AiJsonInput<T> extends AiInput {
    private final Jsonable<T> jsonable;

    public AiJsonInput(Jsonable<T> jsonable) {
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
