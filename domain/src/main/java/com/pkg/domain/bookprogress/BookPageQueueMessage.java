package com.pkg.domain.bookprogress;

public record BookPageQueueMessage(
        String bipId,
        int pageIndex,
        String userInput,
        String characterName,
        String characterDescription,
        String backgroundInfo
) {}
