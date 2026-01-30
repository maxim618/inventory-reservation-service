package com.max.inventory.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest
@Testcontainers
class ReservationLuaScriptIT {

    static final String STOCK_KEY = "stock:sku-1";
    static final int TTL_SECONDS = 30;

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ReservationLuaScript reservationLuaScript;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        redisTemplate.opsForValue().set(STOCK_KEY, "10"); // склад = 10

        System.out.println("INIT KEYS = " + redisTemplate.keys("*"));
    }

    @Test
    void shouldReserveStockAtomically_underConcurrentLoad() throws Exception {
        int threads = 20;
        int attempts = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(attempts);

        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {

            results.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    try {
                        String reservationId = UUID.randomUUID().toString();

                        String reservationKey = "reservation:" + reservationId;
                        String idempotencyKey = "idempotency:" + reservationId;

                        return reservationLuaScript.reserve(
                                STOCK_KEY,
                                reservationKey,
                                idempotencyKey,
                                1,
                                TTL_SECONDS,
                                reservationId
                        );
                    } finally {
                        latch.countDown();
                    }
                }
            }));
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long successCount = results.stream()
                .filter(new Predicate<Future<Boolean>>() {
                    @Override
                    public boolean test(Future<Boolean> f) {
                        try {
                            return f.get();
                        } catch (
                                CancellationException | InterruptedException | ExecutionException e) {
                            System.err.println(e.getMessage());
                            return false;
                        }
                    }
                })
                .count();

        String finalStock = redisTemplate.opsForValue().get(STOCK_KEY);

        assertThat(successCount).isEqualTo(10);
        assertThat(finalStock).isEqualTo("0");
    }
}