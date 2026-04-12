package com.pkg.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookCompleteEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BookCompleteEventHandler.class);

    private final BookInProgressRepositoryAdapter bookInProgressRepositoryAdapter;

    public BookCompleteEventHandler(BookInProgressRepositoryAdapter bookInProgressRepositoryAdapter) {
        this.bookInProgressRepositoryAdapter = bookInProgressRepositoryAdapter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BookCompleteEvent event) {
        try {
            bookInProgressRepositoryAdapter.markAsCompleted(event.bookInProgressId());
        } catch (Exception e) {
            log.warn("[BookComplete] Redis 상태 업데이트 실패 - bipId: {}, 다음 조회 시 DB 기준으로 복구됩니다.",
                    event.bookInProgressId(), e);
        }
    }
}
