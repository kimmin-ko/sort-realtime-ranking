package study.min.sortrealtimeranking.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import study.min.sortrealtimeranking.dto.RankingUpdateMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventPublisher {

    private static final String CHANNEL = "ranking:update";

    private final StringRedisTemplate redisTemplate;

    /**
     * score 변경 이벤트를 Redis Pub/Sub 채널에 발행한다.
     * 메시지 내용은 트리거 역할이므로 userId만 전송한다.
     */
    public void publish(RankingUpdateMessage message) {
        redisTemplate.convertAndSend(CHANNEL, message.userId().toString());
        log.debug("랭킹 변경 이벤트 발행: userId={}", message.userId());
    }
}
