package com.pkg.domain.ai;

import com.pkg.domain.bookprogress.BookToProgress;
import org.springframework.stereotype.Component;

@Component
public interface BookPageGenerator {

    BookPageGenerated generatePageFrom(BookToProgress bookToProgress);
}
