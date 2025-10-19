package com.pkg.domain.uitl;

import java.util.UUID;

public class UuidGen {

    private UuidGen() {}

    public static String compact() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String prefixed(String prefix) {
        return prefix + "_" + compact();
    }

    public static boolean isValid(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
