package org.duhan.webfluxcoroutinepaymentserver.common.config

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@Component
class CacheManager(
    redisTemplate: ReactiveRedisTemplate<Any, Any>,
) {
    private val ops = redisTemplate.opsForValue()

    suspend fun <T> get(key: CacheKey): T? {
        return ops.get(key).awaitSingleOrNull()?.let { it as T }
    }

    suspend fun <T> get(
        key: CacheKey,
        supplier: suspend () -> T?,
    ): T? {
        return get(key) ?: supplier.invoke()?.also { set(key, it) }
    }

    suspend fun set(
        key: CacheKey,
        value: Any,
    ) {
        val ttl =
            TTL[key.group]?.toJavaDuration()
                ?: DEFAULT_TTL
        ops.set(key, value, ttl).awaitSingle()
    }

    suspend fun delete(key: CacheKey) {
        ops.delete(key).awaitSingle()
    }

    companion object {
        val TTL = HashMap<Any, Duration>()
        val DEFAULT_TTL = 10.minutes.toJavaDuration()
    }
}

class CacheKey(val group: Any, vararg elements: Any) : SimpleKey(group, *elements)
