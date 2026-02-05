package com.max.inventory;

import com.max.inventory.mapper.ReservationResultMapper;
import com.max.inventory.redis.ReservationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ReservationResultMapperTest {

    ReservationResultMapper mapper = new ReservationResultMapper();

    @Test
    void should_map_1_to_reserved() {
        assertThat(mapper.map(1L)).isEqualTo(ReservationResult.RESERVED);
    }

    @Test
    void should_map_0_to_insufficient_stock() {
        assertThat(mapper.map(0L)).isEqualTo(ReservationResult.INSUFFICIENT_STOCK);
    }

    @Test
    void should_map_2_to_idempotent() {
        assertThat(mapper.map(2L)).isEqualTo(ReservationResult.IDEMPOTENT);
    }

    @Test
    void should_map_unknown_to_failed() {
        assertThat(mapper.map(99L)).isEqualTo(ReservationResult.FAILED);
    }
}
