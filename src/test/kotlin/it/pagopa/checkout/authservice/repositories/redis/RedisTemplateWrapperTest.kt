package it.pagopa.checkout.authservice.repositories.redis

import java.time.Duration
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

class RedisTemplateWrapperTest {
    class MockRedisTemplateWrapper(
        redisTemplate: RedisTemplate<String, String>,
        ttl: Duration,
        keyspace: String,
    ) :
        RedisTemplateWrapper<String>(
            redisTemplate = redisTemplate,
            ttl = ttl,
            keyspace = keyspace,
        ) {
        override fun getKeyFromEntity(value: String) = "key"
    }

    private val redisTemplate: RedisTemplate<String, String> = mock()
    private val defaultTtl = Duration.ofSeconds(1)
    private val keySpace = "keyspace"

    private val mockedRedisTemplate =
        MockRedisTemplateWrapper(
            redisTemplate = redisTemplate,
            keyspace = keySpace,
            ttl = defaultTtl,
        )

    private val opsForVal: ValueOperations<String, String> = mock()

    @Test
    fun `should save entity`() {
        // pre-conditions
        val value = "value"
        given(redisTemplate.opsForValue()).willReturn(opsForVal)
        doNothing().`when`(opsForVal).set(any(), any(), any<Duration>())
        // test
        mockedRedisTemplate.save(value)
        // assertions
        verify(opsForVal, times(1)).set("$keySpace:key", value, defaultTtl)
    }

    @Test
    fun `should find entity by id`() {
        // pre-conditions
        val value = "value"
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
    fun `should perform delete operation`() {
        // pre-conditions
        val key = "key"
        given(redisTemplate.delete(any<String>())).willReturn(true)
        // test
        val returnedValue = mockedRedisTemplate.delete(key)
        // assertions
        assertEquals(true, returnedValue)
        verify(redisTemplate, times(1)).delete("$keySpace:key")
    }

    @Test
    fun `should find all keys in keyspace`() {
        // pre-conditions

        given(redisTemplate.keys(any<String>())).willReturn(emptySet())
        // test
        val returnedValue = mockedRedisTemplate.keysInKeyspace()
        // assertions
        assertEquals(emptySet(), returnedValue)
        verify(redisTemplate, times(1)).keys("$keySpace:*")
    }

    @Test
    fun `should get all values in keyspace`() {
        // pre-conditions
        val keys = setOf("key1", "key2")
        val values = listOf("value1", "value2")
        given(redisTemplate.keys(any<String>())).willReturn(keys)
        given(redisTemplate.opsForValue()).willReturn(opsForVal)
        given(opsForVal.multiGet(any())).willReturn(values)
        // test
        val returnedValue = mockedRedisTemplate.getAllValues()
        // assertions
        assertEquals(values, returnedValue)
        verify(redisTemplate, times(1)).keys("$keySpace:*")
        verify(opsForVal, times(1)).multiGet(keys)
    }
}
