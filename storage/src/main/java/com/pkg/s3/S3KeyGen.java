package com.pkg.s3;

import com.pkg.domain.uitl.UuidGen;

public class S3KeyGen {

    private static final String BUCKET_HOST = "https://littlewriter.s3.ap-northeast-2.amazonaws.com/";
    private static final String TEMPORARY_KEY_PREFIX = "temporary/";
    private static final String BOOK_KEY_PREFIX = "book/";
    private static final String CHARACTER_KEY_PREFIX = "character/";

    public static String createTempKey(Ext ext) {
        return UuidGen.prefixed(TEMPORARY_KEY_PREFIX) + ext.val;
    }

    public static String createBookKey(Ext ext) {
        return UuidGen.prefixed(BOOK_KEY_PREFIX) + ext.val;
    }

    public static String createCharacterKey(Ext ext) {
        return UuidGen.prefixed(CHARACTER_KEY_PREFIX) + ext.val;
    }

    public static String getBucketHost() {
        return BUCKET_HOST;
    }

    public enum Ext {

        PNG(".png"),
        JPEG(".jpeg"),
        WEBP(".webp"),
        ;

        private final String val;

        Ext(String val) {
            this.val = val;
        }
    }
}
