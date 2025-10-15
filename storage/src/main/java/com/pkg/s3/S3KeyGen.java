package com.pkg.s3;

public class S3KeyGen {

    private static final String TEMPORARY_KEY_PREFIX = "temporary/";
    private static final String BOOK_KEY_PREFIX = "book/";
    private static final String CHARACTER_KEY_PREFIX = "character/";

    public static String temporaryFrom(String key) {
        return TEMPORARY_KEY_PREFIX + key;
    }

    public static String bookFrom(String key) {
        return BOOK_KEY_PREFIX + key;
    }

    public static String characterFrom(String key) {
        return CHARACTER_KEY_PREFIX + key;
    }
}
