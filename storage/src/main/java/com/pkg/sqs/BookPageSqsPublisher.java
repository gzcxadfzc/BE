package com.pkg.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.domain.bookprogress.BookPageQueueMessage;
import com.pkg.domain.bookprogress.BookPageQueuePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class BookPageSqsPublisher implements BookPageQueuePublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${sqs.queue-url:dummy}")
    private String queueUrl;

    public BookPageSqsPublisher(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(BookPageQueueMessage message) {
        String body = toJson(message);
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageGroupId(message.bipId())
                .messageDeduplicationId(message.bipId() + "-" + message.pageIndex())
                .build());
    }

    private String toJson(BookPageQueueMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("SQS 메시지 직렬화 실패: " + message.bipId(), e);
        }
    }
}
