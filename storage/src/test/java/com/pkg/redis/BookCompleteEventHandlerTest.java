package com.pkg.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookCompleteEventHandler 단위 테스트")
class BookCompleteEventHandlerTest {

    @Mock
    private BookInProgressRepositoryAdapter bookInProgressRepositoryAdapter;

    @InjectMocks
    private BookCompleteEventHandler handler;

    @Test
    @DisplayName("handle: 정상 이벤트를 수신하면 markAsCompleted를 호출한다")
    void handle_callsMarkAsCompleted() {
        // Given
        BookCompleteEvent event = new BookCompleteEvent("bip-001");

        // When
        handler.handle(event);

        // Then
        verify(bookInProgressRepositoryAdapter).markAsCompleted("bip-001");
    }

    @Test
    @DisplayName("handle: markAsCompleted에서 예외가 발생해도 삼킨다")
    void handle_swallowsException() {
        // Given
        BookCompleteEvent event = new BookCompleteEvent("bip-error");
        doThrow(new RuntimeException("Redis 연결 실패"))
                .when(bookInProgressRepositoryAdapter).markAsCompleted("bip-error");

        // When & Then - should not throw
        handler.handle(event);

        verify(bookInProgressRepositoryAdapter).markAsCompleted("bip-error");
    }

    @Test
    @DisplayName("handle: 서로 다른 BIP ID가 독립적으로 처리된다")
    void handle_distinctBipIds() {
        // When
        handler.handle(new BookCompleteEvent("bip-001"));
        handler.handle(new BookCompleteEvent("bip-002"));

        // Then
        verify(bookInProgressRepositoryAdapter).markAsCompleted("bip-001");
        verify(bookInProgressRepositoryAdapter).markAsCompleted("bip-002");
    }
}
