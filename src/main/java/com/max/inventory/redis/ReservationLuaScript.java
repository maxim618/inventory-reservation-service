package com.max.inventory.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

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
        Long result = redisTemplate.execute(
                script,
                List.of(stockKey, reservationKey, idempotencyKey),
                String.valueOf(quantity),
                String.valueOf(ttlSeconds),
                reservationId
        );

        return result != null && result == 1L;
    }
}