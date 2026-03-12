package dev.acog.plugin.util

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimiter {
    private val maxRequestsPerMinute = 10
    private val cache = ConcurrentHashMap<String, RateLimitEntry>()
    
    fun checkRateLimit(userId: String): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        val entry = cache.compute(userId) { _, current ->
            if (current == null || currentTimeMillis > current.resetTime) {
                RateLimitEntry(1, currentTimeMillis + 60_000)
            } else {
                current.copy(count = current.count + 1)
            }
        } ?: return false

        return entry.count <= maxRequestsPerMinute
    }

    fun getRemainingRequests(userId: String): Int {
        val entry = cache[userId] ?: return maxRequestsPerMinute
        if (System.currentTimeMillis() > entry.resetTime) {
            return maxRequestsPerMinute
        }
        return maxOf(0, maxRequestsPerMinute - entry.count)
    }

    @Scheduled(fixedRate = 3600000)
    fun cleanup() {
        val currentTimeMillis = System.currentTimeMillis()
        cache.entries.removeIf { it.value.resetTime < currentTimeMillis }
    }

    private data class RateLimitEntry(val count: Int, val resetTime: Long)
}
