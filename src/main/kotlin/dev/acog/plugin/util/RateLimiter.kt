package dev.acog.plugin.util

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimiter {
    private val maxRequestsPerMinute = 10
    private val cache = ConcurrentHashMap<String, RateLimitEntry>()
    
    fun checkRateLimit(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val entry = cache.compute(userId) { _, current ->
            if (current == null || now > current.resetTime) {
                RateLimitEntry(1, now + 60_000)
            } else {
                current.copy(count = current.count + 1)
            }
        }!!
        
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
        val now = System.currentTimeMillis()
        cache.entries.removeIf { it.value.resetTime < now }
    }

    private data class RateLimitEntry(val count: Int, val resetTime: Long)
}
