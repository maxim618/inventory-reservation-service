package com.max.inventory.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ReservationLuaScript {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;

    /**
     * Lua script contract:
     *  - returns 1  → reservation succeeded (or idempotent retry)
     *  - returns 0  → insufficient stock
     *  - returns nil → execution error
     */

    public ReservationLuaScript(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.script = new DefaultRedisScript<>();
        this.script.setScriptSource(
                new ResourceScriptSource(
                        new ClassPathResource("redis/infra/reservation.lua")
                )
        );
        this.script.setResultType(Long.class);
    }

    // return true если резерв успешен
    public boolean reserve(
            String stockKey,
            String reservationKey,
            String idempotencyKey,
            int quantity,
            int ttlSeconds,
            String reservationId
    ) {
        try {
            log.debug(
                    "Trying to reserve stock. stockKey={}, reservationKey={}, qty={}, hasIdempotencyKey={}",
                    stockKey, reservationKey, quantity, Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null")
            );

            Long result =  redisTemplate.execute(
                    script,
                    List.of(stockKey, reservationKey, Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null")),
                    String.valueOf(quantity),
                    String.valueOf(ttlSeconds),
                    reservationId
            );
            log.debug(
                    "Lua result for stockKey={} is {}",
                    stockKey, result
            );

            if (result == null) {
                log.warn(
                        "Lua returned null. stockKey={}, reservationKey={}",
                        stockKey, reservationKey
                );
                return false;
            }

            if (result == 1L) {
                log.debug(
                        "Stock reserved successfully. stockKey={}, reservationKey={}",
                        stockKey, reservationKey
                );
                return true;
            }

            log.debug(
                    "Stock reservation rejected. stockKey={}, reservationKey={}, luaResult={}",
                    stockKey, reservationKey, result
            );
            return false;

        } catch (Exception e) {
            log.error(
                    "Lua reservation failed. stockKey={}, reservationKey={}",
                    stockKey, reservationKey, e
            );
            throw e;
        }
    }
}