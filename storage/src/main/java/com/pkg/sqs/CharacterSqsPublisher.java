package com.pkg.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.domain.character.CharacterQueueMessage;
import com.pkg.domain.character.CharacterQueuePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class CharacterSqsPublisher implements CharacterQueuePublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${sqs.queue-url:dummy}")
    private String queueUrl;

    public CharacterSqsPublisher(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(CharacterQueueMessage message) {
        String body = toJson(message);
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageGroupId("character-" + message.cipId())
                .messageDeduplicationId("character-" + message.cipId())
                .build());
    }

    private String toJson(CharacterQueueMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("SQS 메시지 직렬화 실패: " + message.cipId(), e);
        }
    }
}
