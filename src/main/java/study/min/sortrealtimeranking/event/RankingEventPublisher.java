package study.min.sortrealtimeranking.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import study.min.sortrealtimeranking.dto.RankingUpdateMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventPublisher {

    private static final String CHANNEL = "ranking:update";

    private final StringRedisTemplate redisTemplate;

    /**
     * score 변경 이벤트를 Redis Pub/Sub 채널에 비동기로 발행한다.
     * 별도 스레드에서 실행되므로 호출자의 응답 지연이나 예외에 영향을 주지 않는다.
     */
    @Async
    public void publish(RankingUpdateMessage message) {
        redisTemplate.convertAndSend(CHANNEL, message.userId().toString());
        log.debug("랭킹 변경 이벤트 발행: userId={}", message.userId());
    }
}
