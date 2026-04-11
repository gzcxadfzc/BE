package com.pkg.domain.bookprogress;

public interface BookPageQueuePublisher {
    void publish(BookPageQueueMessage message);
}
