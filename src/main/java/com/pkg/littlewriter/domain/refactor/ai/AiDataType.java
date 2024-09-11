package com.pkg.littlewriter.domain.refactor.ai;

import lombok.Getter;

@Getter
public enum AiDataType {
    IMAGE("image"),
    JSON("json"),
    TEXT("text");

    private final String typeName;

    AiDataType(String name) {
        this.typeName = name;
    }
}
