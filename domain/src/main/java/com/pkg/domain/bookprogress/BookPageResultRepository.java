package com.pkg.domain.bookprogress;

import java.util.Optional;

public interface BookPageResultRepository {
    Optional<BookPageResult> find(String bipId);
    void delete(String bipId);
}
