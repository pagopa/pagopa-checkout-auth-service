package it.pagopa.checkout.authservice.repositories.redis

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

    fun saveIfAbsent(value: V): Boolean? {
        return redisTemplate
            .opsForValue()
            .setIfAbsent("$keyspace:${getKeyFromEntity(value)}", value!!, ttl)
    }

    fun saveIfAbsent(value: V, customTtl: Duration): Boolean? {
        return redisTemplate
            .opsForValue()
            .setIfAbsent("$keyspace:${getKeyFromEntity(value)}", value!!, customTtl)
    }

    fun findById(key: String): V? = redisTemplate.opsForValue()["$keyspace:$key"]

    fun delete(key: String) = redisTemplate.delete(key)

    fun keysInKeyspace(): MutableSet<String> = redisTemplate.keys("$keyspace*")

    protected abstract fun getKeyFromEntity(value: V): String
}
