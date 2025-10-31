package com.pkg.redis;

import com.pkg.config.RedisConfig;
import com.pkg.domain.bookprogress.AiGenerateResult;
import com.pkg.domain.bookprogress.BookInProgress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataRedisTest
@Import({
        RedisConfig.class,
        RedisLockManager.class,
        BookInProgressLockExecutorAdapter.class
})
class BookInProgressLockExecutorAdapterTest {

    @Autowired
    private BookInProgressLockExecutorAdapter lockExecutorAdapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String TEST_BIP_ID = "test-bip-123";
    private static final String BIP_LOCK_KEY_PREFIX = "bip:lock:";

    @BeforeEach
    void setUp() {
        // Clean up any existing locks before each test
        redisTemplate.delete(BIP_LOCK_KEY_PREFIX + TEST_BIP_ID);
    }

    @AfterEach
    void cleanup() {
        // Clean up locks after each test
        redisTemplate.delete(BIP_LOCK_KEY_PREFIX + TEST_BIP_ID);
    }

    @Test
    @DisplayName("단일 스레드 - 락 획득 후 작업 수행 및 자동 해제")
    void testSingleThreadLockAcquisitionAndRelease() {
        // Given
        String bipId = TEST_BIP_ID;
        BookInProgress mockBookInProgress = createMockBookInProgress();
        Supplier<AiGenerateResult> generator = () -> new AiGenerateResult(mockBookInProgress, List.of("question1"));

        // When
        AiGenerateResult result = lockExecutorAdapter.updateWithLock(bipId, generator);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.bookInProgress()).isEqualTo(mockBookInProgress);
        assertThat(result.questions()).containsExactly("question1");

        // Verify lock is released
        Boolean hasKey = redisTemplate.hasKey(BIP_LOCK_KEY_PREFIX + bipId);
        assertThat(hasKey).isFalse();
    }

    @Test
    @DisplayName("다중 스레드 - 동시에 같은 리소스에 대한 락 획득 시도 시 하나만 성공")
    void testMultiThreadConcurrentLockAcquisition() throws InterruptedException {
        // Given
        String bipId = TEST_BIP_ID;
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // When - 모든 스레드가 동시에 락 획득 시도
        for (int i = 0; i < threadCount; i++) {
            int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기

                    BookInProgress mockBookInProgress = createMockBookInProgress();
                    Supplier<AiGenerateResult> generator = () -> {
                        executionOrder.add("Thread-" + threadNum);
                        // Simulate work
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return new AiGenerateResult(mockBookInProgress, List.of("question"));
                    };

                    lockExecutorAdapter.updateWithLock(bipId, generator);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Lock acquisition failed
                    failureCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 시작
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1); // Only one thread should succeed
        assertThat(failureCount.get()).isEqualTo(threadCount - 1); // Others should fail
        assertThat(executionOrder).hasSize(1); // Only one thread executed the work
    }

    @Test
    @DisplayName("다중 스레드 - 순차적으로 락 획득 및 해제 (락 재사용)")
    void testMultiThreadSequentialLockAcquisition() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        String bipId = TEST_BIP_ID;
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<AiGenerateResult>> futures = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);

        // When - 스레드들이 순차적으로 락을 획득하고 작업 수행
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            Future<AiGenerateResult> future = executorService.submit(() -> {
                BookInProgress mockBookInProgress = createMockBookInProgress();
                Supplier<AiGenerateResult> generator = () -> {
                    int value = counter.incrementAndGet();
                    // Simulate work
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new AiGenerateResult(mockBookInProgress, List.of("question-" + threadNum, "value-" + value));
                };

                // Wait a bit to ensure sequential execution
                try {
                    Thread.sleep(threadNum * 100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return lockExecutorAdapter.updateWithLock(bipId, generator);
            });
            futures.add(future);
        }

        // Then - 모든 스레드가 성공적으로 작업을 완료해야 함
        List<AiGenerateResult> results = new ArrayList<>();
        for (Future<AiGenerateResult> future : futures) {
            AiGenerateResult result = future.get(10, TimeUnit.SECONDS);
            results.add(result);
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(results).hasSize(threadCount);
        assertThat(counter.get()).isEqualTo(threadCount);

        // Verify all threads completed successfully
        for (int i = 0; i < threadCount; i++) {
            AiGenerateResult result = results.get(i);
            assertThat(result).isNotNull();
            assertThat(result.questions()).hasSize(2);
            assertThat(result.questions().get(0)).isEqualTo("question-" + i);
        }
    }

    @Test
    @DisplayName("다중 스레드 - 서로 다른 리소스에 대한 동시 락 획득")
    void testMultiThreadDifferentResourcesLockAcquisition() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        int resourceCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(resourceCount);
        List<Future<AiGenerateResult>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);

        // When - 각 스레드가 서로 다른 리소스에 대해 락 획득
        for (int i = 0; i < resourceCount; i++) {
            final String bipId = "test-bip-" + i;
            final int threadNum = i;

            Future<AiGenerateResult> future = executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작

                    BookInProgress mockBookInProgress = createMockBookInProgress();
                    Supplier<AiGenerateResult> generator = () -> {
                        // Simulate work
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return new AiGenerateResult(mockBookInProgress, List.of("result-" + threadNum));
                    };

                    return lockExecutorAdapter.updateWithLock(bipId, generator);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        startLatch.countDown(); // 모든 스레드 동시 시작

        // Then - 모든 스레드가 성공해야 함 (서로 다른 리소스이므로)
        List<AiGenerateResult> results = new ArrayList<>();
        for (Future<AiGenerateResult> future : futures) {
            AiGenerateResult result = future.get(10, TimeUnit.SECONDS);
            results.add(result);
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(results).hasSize(resourceCount);
        for (int i = 0; i < resourceCount; i++) {
            assertThat(results.get(i)).isNotNull();
            assertThat(results.get(i).questions()).contains("result-" + i);
        }

        // Clean up
        for (int i = 0; i < resourceCount; i++) {
            redisTemplate.delete(BIP_LOCK_KEY_PREFIX + "test-bip-" + i);
        }
    }

    @Test
    @DisplayName("락 획득 실패 시 예외 발생")
    void testLockAcquisitionFailureThrowsException() throws InterruptedException {
        // Given
        String bipId = TEST_BIP_ID;
        CountDownLatch lockHeldLatch = new CountDownLatch(1);
        CountDownLatch testCompleteLatch = new CountDownLatch(1);

        // 첫 번째 스레드가 락을 획득하고 유지
        Thread lockHoldingThread = new Thread(() -> {
            BookInProgress mockBookInProgress = createMockBookInProgress();
            Supplier<AiGenerateResult> generator = () -> {
                lockHeldLatch.countDown();
                try {
                    testCompleteLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new AiGenerateResult(mockBookInProgress, List.of("question"));
            };

            lockExecutorAdapter.updateWithLock(bipId, generator);
        });

        lockHoldingThread.start();
        lockHeldLatch.await(); // 첫 번째 스레드가 락을 획득할 때까지 대기

        // When & Then - 두 번째 스레드가 락 획득 시도 시 예외 발생
        BookInProgress mockBookInProgress = createMockBookInProgress();
        Supplier<AiGenerateResult> generator = () -> new AiGenerateResult(mockBookInProgress, List.of("question"));

        assertThatThrownBy(() -> lockExecutorAdapter.updateWithLock(bipId, generator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 해당 자원에 대한 작업이 진행 중입니다");

        // Clean up
        testCompleteLatch.countDown();
        lockHoldingThread.join(5000);
    }

    @Test
    @DisplayName("작업 중 예외 발생 시에도 락이 해제됨")
    void testLockReleasedOnException() {
        // Given
        String bipId = TEST_BIP_ID;
        Supplier<AiGenerateResult> failingGenerator = () -> {
            throw new RuntimeException("Simulated error");
        };

        // When & Then - 예외 발생
        assertThatThrownBy(() -> lockExecutorAdapter.updateWithLock(bipId, failingGenerator))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated error");

        // Verify lock is released even after exception
        Boolean hasKey = redisTemplate.hasKey(BIP_LOCK_KEY_PREFIX + bipId);
        assertThat(hasKey).isFalse();

        // Verify another thread can acquire the lock
        BookInProgress mockBookInProgress = createMockBookInProgress();
        Supplier<AiGenerateResult> successGenerator = () -> new AiGenerateResult(mockBookInProgress, List.of("question"));

        AiGenerateResult result = lockExecutorAdapter.updateWithLock(bipId, successGenerator);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("다중 스레드 - 고부하 상황에서의 락 동작 검증 (20 threads)")
    void testHighConcurrencyLockBehavior() throws InterruptedException {
        // Given
        String bipId = TEST_BIP_ID;
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger executionCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // When - 고부하 상황 시뮬레이션
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    BookInProgress mockBookInProgress = createMockBookInProgress();
                    Supplier<AiGenerateResult> generator = () -> {
                        int count = executionCount.incrementAndGet();
                        // Simulate work with variable duration
                        try {
                            Thread.sleep(50 + (count % 3) * 10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return new AiGenerateResult(mockBookInProgress, List.of("question-" + count));
                    };

                    lockExecutorAdapter.updateWithLock(bipId, generator);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(60, TimeUnit.SECONDS);

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
        assertThat(executionCount.get()).isEqualTo(1);

        // Verify lock is completely released
        Boolean hasKey = redisTemplate.hasKey(BIP_LOCK_KEY_PREFIX + bipId);
        assertThat(hasKey).isFalse();
    }

    @Test
    @DisplayName("다중 스레드 - 재시도 로직 시뮬레이션 (락 해제 후 재획득)")
    void testRetryLogicAfterLockRelease() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        String bipId = TEST_BIP_ID;
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch thread1StartLatch = new CountDownLatch(1);
        CountDownLatch thread1CompleteLatch = new CountDownLatch(1);

        // When - Thread 1이 먼저 락을 획득하고 작업 수행
        Future<AiGenerateResult> future1 = executorService.submit(() -> {
            thread1StartLatch.countDown();
            BookInProgress mockBookInProgress = createMockBookInProgress();
            Supplier<AiGenerateResult> generator = () -> {
                try {
                    Thread.sleep(200); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new AiGenerateResult(mockBookInProgress, List.of("thread1-result"));
            };
            AiGenerateResult result = lockExecutorAdapter.updateWithLock(bipId, generator);
            thread1CompleteLatch.countDown();
            return result;
        });

        thread1StartLatch.await(); // Thread 1이 시작될 때까지 대기
        Thread.sleep(50); // Thread 1이 락을 확실히 획득하도록 대기

        // Thread 2가 실패하고, Thread 1 완료 후 재시도하여 성공
        Future<AiGenerateResult> future2 = executorService.submit(() -> {
            BookInProgress mockBookInProgress = createMockBookInProgress();
            Supplier<AiGenerateResult> generator = () ->
                new AiGenerateResult(mockBookInProgress, List.of("thread2-result"));

            // First attempt - should fail
            try {
                lockExecutorAdapter.updateWithLock(bipId, generator);
                return null; // Should not reach here
            } catch (IllegalStateException e) {
                // Expected failure
                try {
                    thread1CompleteLatch.await(5, TimeUnit.SECONDS); // Wait for thread 1 to complete
                    Thread.sleep(100); // Small delay to ensure lock is released
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Retry - should succeed
                return lockExecutorAdapter.updateWithLock(bipId, generator);
            }
        });

        // Then
        AiGenerateResult result1 = future1.get(10, TimeUnit.SECONDS);
        AiGenerateResult result2 = future2.get(10, TimeUnit.SECONDS);

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(result1).isNotNull();
        assertThat(result1.questions()).containsExactly("thread1-result");

        assertThat(result2).isNotNull();
        assertThat(result2.questions()).containsExactly("thread2-result");
    }

    // Helper method to create mock BookInProgress
    private BookInProgress createMockBookInProgress() {
        return new BookInProgress(
                TEST_BIP_ID,
                1L,
                "test background",
                null,
                List.of(),
                BookInProgress.Status.IN_PROGRESS
        );
    }
}
