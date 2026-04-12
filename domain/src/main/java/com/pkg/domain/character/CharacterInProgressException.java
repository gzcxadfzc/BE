package com.pkg.domain.character;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;

public class CharacterInProgressException extends DomainException {

    public CharacterInProgressException(DomainExceptionCode code, String message) {
        super(code, message);
    }

    public static CharacterInProgressException notFound(String cipId) {
        return new CharacterInProgressException(DomainExceptionCode.E404, cipId + " not found");
    }

    public static CharacterInProgressException alreadyCompleted(String cipId) {
        return new CharacterInProgressException(DomainExceptionCode.E409, cipId + " is already completed");
    }

    public static CharacterInProgressException resultNotReady(String cipId) {
        return new CharacterInProgressException(DomainExceptionCode.E409, cipId + " result is not ready yet");
    }

    public static CharacterInProgressException forbidden() {
        return new CharacterInProgressException(DomainExceptionCode.E403, "not authorized");
    }
}
