package com.pkg.domain.bookprogress;

import com.pkg.domain.book.Book;
import org.springframework.stereotype.Component;
import java.util.function.Supplier;

@Component
public interface BookInProgressLockExecutor {

    AiGenerateResult updateWithLock(String lockKey, Supplier<AiGenerateResult> generator);

    Book saveWithLock(String lockKey, Supplier<Book> generator);
}
