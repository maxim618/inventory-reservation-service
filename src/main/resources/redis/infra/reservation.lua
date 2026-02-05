-- KEYS[1] = stock key
-- KEYS[2] = reservation key
-- KEYS[3] = idempotency key

-- ARGV[1] = quantity
-- ARGV[2] = ttlSeconds
-- ARGV[3] = reservationId

-- 1 = SUCCESS
-- 0 = INSUFFICIENT_STOCK
-- 2 = IDEMPOTENT_HIT

local stockKey = KEYS[1]
local reservationKey = KEYS[2]
local idempotencyKey = KEYS[3]

local quantity = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])
local reservationId = ARGV[3]

-- idempotency
if redis.call("EXISTS", idempotencyKey) == 1 then
    return 2
end

local available = tonumber(redis.call("GET", stockKey))
if not available or available < quantity then
    return 0
end

redis.call("DECRBY", stockKey, quantity)

redis.call("HSET", reservationKey,
        "reservationId", reservationId,
        "quantity", quantity,
        "status", "RESERVED"
)
redis.call("EXPIRE", reservationKey, ttl)

redis.call("SET", idempotencyKey, reservationId)
redis.call("EXPIRE", idempotencyKey, ttl)

--проверка
redis.call("SET", "debug:available", tostring(available))

return 1
