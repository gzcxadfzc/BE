package com.pkg.domain.bookprogress;

public interface BookInProgressPendingGuard {
    void set(String bipId);
    void delete(String bipId);
    boolean exists(String bipId);
}
