package it.pagopa.checkout.authservice.repositories.redis

import java.time.Duration
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.reactivestreams.Publisher
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class RedisTemplateWrapperTest {
    class MockRedisTemplateWrapper(
        reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
        ttl: Duration,
        keyspace: String,
    ) :
        ReactiveRedisTemplateWrapper<String>(
            reactiveRedisTemplate = reactiveRedisTemplate,
            ttl = ttl,
            keyspace = keyspace,
        ) {
        override fun getKeyFromEntity(value: String) = "key"
    }

    private val redisTemplate: ReactiveRedisTemplate<String, String> = mock()
    private val defaultTtl = Duration.ofSeconds(1)
    private val keySpace = "keyspace"

    private val mockedRedisTemplate =
        MockRedisTemplateWrapper(
            reactiveRedisTemplate = redisTemplate,
            keyspace = keySpace,
            ttl = defaultTtl,
        )

    private val opsForVal: ReactiveValueOperations<String, String> = mock()

    @Test
    fun `should save entity`() {
        // pre-conditions
        val value = "value"
        given(redisTemplate.opsForValue()).willReturn(opsForVal)
        given(opsForVal.set(any(), any(), any<Duration>())).willReturn(Mono.empty())
        // test
        mockedRedisTemplate.save(value)
        // assertions
        verify(opsForVal, times(1)).set("$keySpace:key", value, defaultTtl)
    }

    @Test
    fun `should find entity by id`() {
        // pre-conditions
        val value = Mono.just("value")
        val key = "key"
        given(redisTemplate.opsForValue()).willReturn(opsForVal)
        given(opsForVal.get(any())).willReturn(value)
        // test
        val returnedValue = mockedRedisTemplate.findById(key)
        // assertions
        assertEquals(value, returnedValue)
        verify(opsForVal, times(1)).get("$keySpace:key")
    }

    @Test
    fun `should perform delete by id operation`() {
        // pre-conditions
        val key = "key"
        given(redisTemplate.delete(any<String>())).willReturn(Mono.just(1))
        // test
        val returnedValue = mockedRedisTemplate.deleteById(key)
        // assertions
        StepVerifier.create(returnedValue).expectNext(true).verifyComplete()
        verify(redisTemplate, times(1)).delete("$keySpace:key")
    }

    @Test
    fun `should perform delete all keys operation`() {
        // pre-conditions
        val keys = Flux.just("key1", "key2")
        given(redisTemplate.keys(any())).willReturn(keys)
        given(redisTemplate.delete(any<Publisher<String>>())).willReturn(Mono.just(2L))
        // test
        val returnedValue = mockedRedisTemplate.deleteAll()
        // assertions on returned value
        StepVerifier.create(returnedValue).expectNext(2L).verifyComplete()

        // ArgumentCaptor for the Publisher<String> passed to delete
        val captor = argumentCaptor<Publisher<String>>()
        verify(redisTemplate, times(1)).delete(captor.capture())
        verify(redisTemplate, times(1)).keys("$keySpace:*")

        // Now verify that the captured Publisher emits exactly the keys we expect
        StepVerifier.create(captor.firstValue)
            .expectNext("key1")
            .expectNext("key2")
            .verifyComplete()
    }

    @Test
    fun `should find all keys in keyspace`() {
        // pre-conditions
        val keys = listOf<String>()

        given(redisTemplate.keys(any<String>())).willReturn(Flux.fromIterable(keys))
        // test
        val returnedValue = mockedRedisTemplate.keysInKeyspace()
        // assertions
        StepVerifier.create(returnedValue.collectList()).expectNext(keys).verifyComplete()
        verify(redisTemplate, times(1)).keys("$keySpace:*")
    }

    @Test
    fun `should get all values in keyspace`() {
        // pre-conditions
        val keys = setOf("key1", "key2")
        val values = listOf("value1", "value2")
        given(redisTemplate.keys(any<String>())).willReturn(Flux.fromIterable(keys))
        given(redisTemplate.opsForValue()).willReturn(opsForVal)
        given(opsForVal.multiGet(any())).willReturn(Mono.just(values))
        // test
        val returnedValue = mockedRedisTemplate.getAllValues()
        // assertions
        StepVerifier.create(returnedValue).expectNext(values[0], values[1]).verifyComplete()

        verify(redisTemplate, times(1)).keys("$keySpace:*")

        val captor = argumentCaptor<Collection<String>>()
        verify(opsForVal, times(1)).multiGet(captor.capture())
        assertTrue(captor.firstValue.containsAll(keys) && keys.containsAll(captor.firstValue))
    }
}
