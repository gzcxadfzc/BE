package com.pkg.domain.bookprogress;

import com.pkg.domain.book.Book;
import org.springframework.stereotype.Component;
import java.util.function.Supplier;

@Component
public interface BookInProgressLockExecutor {

    <T> T updateWithLock(String lockKey, Supplier<T> action);

    Book saveWithLock(String lockKey, Supplier<Book> generator);
}
