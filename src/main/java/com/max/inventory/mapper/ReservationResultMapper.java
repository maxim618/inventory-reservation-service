package com.max.inventory.mapper;

import com.max.inventory.redis.ReservationResult;
import org.springframework.stereotype.Component;

@Component
public class ReservationResultMapper {

    public ReservationResult map(Long result) {
        if (result == null) {
            return ReservationResult.FAILED;
        }
        return switch (result.intValue()) {
            case 1 -> ReservationResult.RESERVED;
            case 0 -> ReservationResult.INSUFFICIENT_STOCK;
            case 2 -> ReservationResult.IDEMPOTENT;
            default -> ReservationResult.FAILED;
        };
    }
}
