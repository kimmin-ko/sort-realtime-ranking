package study.min.sortrealtimeranking.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import study.min.sortrealtimeranking.dto.RankingListResponse;
import study.min.sortrealtimeranking.service.RankingService;
import study.min.sortrealtimeranking.sse.SseEmitterManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RankingEventSubscriber implements MessageListener {

    private static final int TOP_SIZE = 100;
    private static final long DEBOUNCE_MS = 500; // 500ms 간격으로 최대 1회 처리

    private final RankingService rankingService;
    private final SseEmitterManager sseEmitterManager;

    private final AtomicBoolean pending = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public RankingEventSubscriber(RankingService rankingService, SseEmitterManager sseEmitterManager) {
        this.rankingService = rankingService;
        this.sseEmitterManager = sseEmitterManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.debug("랭킹 변경 이벤트 수신: {}", new String(message.getBody()));

        // 이미 예약된 브로드캐스트가 있으면 무시 (디바운스)
        if (pending.compareAndSet(false, true)) {
            scheduler.schedule(this::broadcastRankings, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void broadcastRankings() {
        pending.set(false);
        RankingListResponse rankings = rankingService.getTopRankings(TOP_SIZE);
        sseEmitterManager.broadcast("ranking", rankings);
    }
}
