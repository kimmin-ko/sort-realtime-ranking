package study.min.sortrealtimeranking.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import study.min.sortrealtimeranking.dto.RankingListResponse;
import study.min.sortrealtimeranking.service.RankingService;
import study.min.sortrealtimeranking.sse.SseEmitterManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventSubscriber implements MessageListener {

    private static final int TOP_SIZE = 100;

    private final RankingService rankingService;
    private final SseEmitterManager sseEmitterManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.debug("랭킹 변경 이벤트 수신: {}", new String(message.getBody()));

        // 최신 상위 랭킹 조회 후 SSE로 브로드캐스트
        RankingListResponse rankings = rankingService.getTopRankings(TOP_SIZE);
        sseEmitterManager.broadcast("ranking", rankings);
    }
}
