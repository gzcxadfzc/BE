package com.pkg.s3;

public enum S3KeyPrefix {
    TEMPORARY("temporary/"),
    CHARACTER("character/"),
    BOOK("book/");
    private final String directoryName;

     S3KeyPrefix(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getPrefix() {
         return this.directoryName;
    }
}
