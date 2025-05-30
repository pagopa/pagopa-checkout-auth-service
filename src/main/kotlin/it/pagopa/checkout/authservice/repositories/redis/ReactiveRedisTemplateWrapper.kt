package it.pagopa.checkout.authservice.repositories.redis

import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

abstract class ReactiveRedisTemplateWrapper<V : Any>(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, V>,
    private val keyspace: String,
    private val ttl: Duration,
) {

    fun save(value: V): Mono<Boolean> {
        val key = "$keyspace:${getKeyFromEntity(value)}"
        return reactiveRedisTemplate.opsForValue().set(key, value, ttl)
    }

    fun findById(key: String): Mono<V> {
        val fullKey = "$keyspace:$key"
        return reactiveRedisTemplate.opsForValue().get(fullKey)
    }

    fun deleteById(key: String): Mono<Boolean> {
        val fullKey = "$keyspace:$key"
        return reactiveRedisTemplate.delete(fullKey).map { it > 0 }
    }

    fun deleteAll(): Mono<Long> {
        return reactiveRedisTemplate.delete(keysInKeyspace())
    }

    fun keysInKeyspace(): Flux<String> {
        return reactiveRedisTemplate.keys("$keyspace:*")
    }

    fun getAllValues(): Flux<V> {
        return keysInKeyspace().collectList().flatMapMany { keys ->
            if (keys.isEmpty()) Flux.empty()
            else
                reactiveRedisTemplate.opsForValue().multiGet(keys).flatMapMany {
                    Flux.fromIterable(it.filterNotNull())
                }
        }
    }

    protected abstract fun getKeyFromEntity(value: V): String
}
