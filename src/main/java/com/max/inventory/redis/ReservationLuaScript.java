package com.max.inventory.redis;

import com.max.inventory.mapper.ReservationResultMapper;
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
    private final ReservationResultMapper reservationResultMapper;

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
        reservationResultMapper = new ReservationResultMapper();
    }

    // return true если резерв успешен
    public ReservationResult reserve(
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
                    stockKey, reservationKey, quantity, idempotencyKey != null
            );

            Long result =  redisTemplate.execute(
                    script,
                    List.of(stockKey, reservationKey, Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null")),
                    String.valueOf(quantity),
                    String.valueOf(ttlSeconds),
                    reservationId
            );
            ReservationResult mapped = reservationResultMapper.map(result);
            log.debug(
                    "Lua reservation result. stockKey={}, reservationKey={}, result={}",
                    stockKey, result, mapped
            );

            return mapped;

        } catch (Exception e) {
            log.error(
                    "Lua reservation failed. stockKey={}, reservationKey={}",
                    stockKey, reservationKey, e
            );
            return ReservationResult.FAILED;
        }
    }
}