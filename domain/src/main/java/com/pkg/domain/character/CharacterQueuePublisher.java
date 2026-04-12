package com.pkg.domain.character;

public interface CharacterQueuePublisher {
    void publish(CharacterQueueMessage message);
}
