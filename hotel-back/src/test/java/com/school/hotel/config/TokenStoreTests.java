package com.school.hotel.config;

import com.school.hotel.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenStoreTests {
    @Test void productionRotationFailureIsRetryableAndDoesNotUseSeparateDelete() {
        var redis=mock(StringRedisTemplate.class);var provider=mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),any(),any()))
            .thenThrow(new IllegalStateException("write unavailable")).thenReturn(1L);
        var store=new TokenStore(provider,true);
        assertEquals(503,assertThrows(BusinessException.class,()->store.rotate("old","new","3",Duration.ofMinutes(1))).getCode());
        assertTrue(store.rotate("old","new","3",Duration.ofMinutes(1)));
        verify(redis,never()).delete(anyString());verify(redis,never()).opsForValue();
    }
    @Test void rotationKeepsSingleSuccessAndRejectsWrongOwner() {
        var store=new TokenStore(mock(ObjectProvider.class),false);
        store.save("old","3",Duration.ofMinutes(1));
        assertFalse(store.rotate("old","wrong","4",Duration.ofMinutes(1)));
        assertEquals("3",store.get("old"));
        assertTrue(store.rotate("old","new","3",Duration.ofMinutes(1)));
        assertFalse(store.rotate("old","new2","3",Duration.ofMinutes(1)));
        assertNull(store.get("old"));assertEquals("3",store.get("new"));
    }
    @Test void productionNeverFallsBackForFailedRedisOperations() {
        var redis=mock(StringRedisTemplate.class);
        var provider=mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("unavailable"));
        when(redis.delete(anyString())).thenThrow(new IllegalStateException("unavailable"));
        var store=new TokenStore(provider,true);
        for(Runnable operation: new Runnable[]{()->store.get("key"),()->store.save("key","value",Duration.ofMinutes(1)),
            ()->store.putIfAbsent("key","value",Duration.ofMinutes(1)),()->store.delete("key"),()->store.consume("key")}) {
            assertEquals(503,assertThrows(BusinessException.class,operation::run).getCode());
        }
    }
    @Test void developmentCanUseMemoryWhenRedisUnavailable() {
        var provider=mock(ObjectProvider.class);
        var store=new TokenStore(provider,false);
        assertTrue(store.putIfAbsent("key","value",Duration.ofMinutes(1)));
        assertFalse(store.putIfAbsent("key","other",Duration.ofMinutes(1)));
        assertEquals("value",store.get("key"));store.delete("key");assertNull(store.get("key"));
    }
    @Test void productionRejectsMissingRedisProvider() {
        var store=new TokenStore(mock(ObjectProvider.class),true);
        assertEquals(503,assertThrows(BusinessException.class,()->store.get("key")).getCode());
    }
}
