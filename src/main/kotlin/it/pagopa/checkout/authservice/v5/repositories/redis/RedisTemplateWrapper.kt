package it.pagopa.checkout.authservice.v5.repositories.redis

import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

abstract class RedisTemplateWrapper<V>(
    private val redisTemplate: RedisTemplate<String, V>,
    private val keyspace: String,
    private val ttl: Duration,
) {

    fun save(value: V) {
        redisTemplate.opsForValue()["$keyspace:${getKeyFromEntity(value)}", value!!] = ttl
    }

    fun findById(key: String): V? = redisTemplate.opsForValue()["$keyspace:$key"]

    fun deleteById(key: String): Boolean = redisTemplate.delete("$keyspace:$key")

    fun deleteAll(): Long = redisTemplate.delete(keysInKeyspace())

    fun keysInKeyspace(): Set<String> = redisTemplate.keys("$keyspace:*").toSet()

    fun getAllValues(): List<V> =
        redisTemplate.opsForValue().multiGet(keysInKeyspace())?.toList() ?: emptyList()

    abstract fun getKeyFromEntity(value: V): String
}
