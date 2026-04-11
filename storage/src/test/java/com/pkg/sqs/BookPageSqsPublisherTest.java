package com.pkg.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.domain.bookprogress.BookPageQueueMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookPageSqsPublisher 단위 테스트")
class BookPageSqsPublisherTest {

    @Mock
    private SqsClient sqsClient;

    private BookPageSqsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new BookPageSqsPublisher(sqsClient, new ObjectMapper());
        ReflectionTestUtils.setField(publisher, "queueUrl", "https://sqs.ap-northeast-2.amazonaws.com/123456789/test.fifo");
    }

    @Test
    @DisplayName("publish: SQS sendMessage 요청이 올바른 파라미터로 전송된다")
    void testPublish() {
        // Given
        BookPageQueueMessage message = new BookPageQueueMessage(
                "bip-001", 2, "오늘 숲에서 토끼를 만났어",
                "토끼 토리", "숲속에 사는 착한 토끼", "숲속 친구들의 모험"
        );

        // When
        publisher.publish(message);

        // Then
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.messageGroupId()).isEqualTo("bip-001");
        assertThat(request.messageDeduplicationId()).isEqualTo("bip-001-2");
        assertThat(request.messageBody()).contains("bip-001");
        assertThat(request.messageBody()).contains("오늘 숲에서 토끼를 만났어");
    }

    @Test
    @DisplayName("publish: MessageGroupId는 bipId, MessageDeduplicationId는 bipId-pageIndex 형식이다")
    void testDeduplicationId() {
        // Given
        BookPageQueueMessage message = new BookPageQueueMessage(
                "abc-xyz", 0, "input", "char", "desc", "bg"
        );

        // When
        publisher.publish(message);

        // Then
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.messageGroupId()).isEqualTo("abc-xyz");
        assertThat(request.messageDeduplicationId()).isEqualTo("abc-xyz-0");
    }
}
