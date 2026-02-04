package com.max.inventory.redis;

public enum ReservationResult {
    RESERVED,              // успех
    INSUFFICIENT_STOCK,    // нет остатков
    IDEMPOTENT,             // повторный запрос
    FAILED                  // ошибка / неизвестно
}
