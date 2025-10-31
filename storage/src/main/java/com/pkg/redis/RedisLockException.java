package com.pkg.redis;

public class RedisLockException extends RuntimeException {

    public RedisLockException(String message) {
        super(message);
    }
}
