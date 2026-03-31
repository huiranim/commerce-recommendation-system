package com.commerce.processor.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks IdempotencyService idempotencyService;

    @Test
    void 신규_이벤트_처리_허용() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("processed:event:evt-1"), eq("1"), any())).thenReturn(true);

        assertThat(idempotencyService.tryMarkProcessed("evt-1")).isTrue();
    }

    @Test
    void 중복_이벤트_거부() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("processed:event:evt-1"), eq("1"), any())).thenReturn(false);

        assertThat(idempotencyService.tryMarkProcessed("evt-1")).isFalse();
    }

    @Test
    void unmark_삭제_호출() {
        idempotencyService.unmarkProcessed("evt-1");
        verify(redisTemplate).delete("processed:event:evt-1");
    }
}
