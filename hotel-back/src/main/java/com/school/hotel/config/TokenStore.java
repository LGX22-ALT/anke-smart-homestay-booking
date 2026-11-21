package com.school.hotel.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TokenStore.class);
    private final StringRedisTemplate redis;
    private final Map<String, MemoryValue> memory = new ConcurrentHashMap<>();
    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> ROTATE =
        new org.springframework.data.redis.core.script.DefaultRedisScript<>(
            "if redis.call('GET',KEYS[1]) ~= ARGV[1] then return 0 end " +
            "redis.call('SET',KEYS[2],ARGV[1],'PX',ARGV[2]); redis.call('DEL',KEYS[1]); return 1",Long.class);
    private final boolean memoryFallback;
    public TokenStore(org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> provider,
                      @org.springframework.beans.factory.annotation.Value("${hotel.redis.required:false}") boolean required) {
        this.redis = provider.getIfAvailable(); this.memoryFallback = !required;
    }
    public void save(String key, String value, Duration ttl) {
        requireStore();
        if (redis != null) { try { redis.opsForValue().set(key, value, ttl); return; } catch (Exception failure) { unavailable(failure); } }
        synchronized(memory) { memory.put(key, new MemoryValue(value, System.currentTimeMillis() + ttl.toMillis())); }
    }
    public boolean putIfAbsent(String key, String value, Duration ttl) {
        requireStore();
        if (redis != null) {
            try { return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl)); }
            catch (Exception failure) { unavailable(failure); }
        }
        MemoryValue current = memory.get(key);
        if (current != null && current.expiresAt() <= System.currentTimeMillis()) memory.remove(key, current);
        return memory.putIfAbsent(key, new MemoryValue(value, System.currentTimeMillis() + ttl.toMillis())) == null;
    }
    public String get(String key) {
        requireStore();
        if (redis != null) { try { return redis.opsForValue().get(key); } catch (Exception failure) { unavailable(failure); } }
        MemoryValue value = memory.get(key);
        if (value == null) return null;
        if (value.expiresAt() <= System.currentTimeMillis()) {
            memory.remove(key, value);
            return null;
        }
        return value.value();
    }
    public void delete(String key) {
        requireStore();
        if (redis != null) { try { redis.delete(key); } catch (Exception failure) { unavailable(failure); } }
        synchronized(memory) { memory.remove(key); }
    }
    public String consume(String key) {
        requireStore();
        if(redis!=null) { try { return redis.opsForValue().getAndDelete(key); } catch(Exception failure) { unavailable(failure); } }
        MemoryValue value=memory.remove(key);
        return value==null || value.expiresAt()<=System.currentTimeMillis()?null:value.value();
    }
    public boolean rotate(String oldKey,String newKey,String value,Duration ttl) {
        requireStore();
        if(redis!=null) {
            try { return Long.valueOf(1).equals(redis.execute(ROTATE,java.util.List.of(oldKey,newKey),value,String.valueOf(ttl.toMillis()))); }
            catch(Exception failure) { unavailable(failure); }
        }
        synchronized(memory) {
            MemoryValue old=memory.get(oldKey);
            if(old==null || old.expiresAt()<=System.currentTimeMillis() || !old.value().equals(value))return false;
            memory.put(newKey,new MemoryValue(value,System.currentTimeMillis()+ttl.toMillis()));memory.remove(oldKey);return true;
        }
    }
    private void requireStore() { if(redis==null && !memoryFallback) unavailable(null); }
    private void unavailable(Exception failure) {
        if(!memoryFallback) {
            log.error("Required Redis store unavailable ({})", failure==null?"missing provider":failure.getClass().getSimpleName());
            throw new com.school.hotel.common.BusinessException(503,"会话服务暂不可用，请稍后重试");
        }
        log.warn("Redis unavailable; using development memory store ({})",failure.getClass().getSimpleName());
    }
    private record MemoryValue(String value, long expiresAt) { }
}
