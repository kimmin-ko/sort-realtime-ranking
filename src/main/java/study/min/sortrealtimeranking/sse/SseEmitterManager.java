package study.min.sortrealtimeranking.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L; // 1시간

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet(); // O(1) add/remove

    /**
     * SseEmitter를 생성만 하고 브로드캐스트 리스트에는 등록하지 않는다.
     * 초기 데이터 전송 후 activate()를 호출해야 브로드캐스트 대상이 된다.
     */
    public SseEmitter create() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> emitters.remove(emitter)); // 클라이언트가 정상적으로 연결을 종료한 경우
        emitter.onTimeout(() -> emitters.remove(emitter));    // SSE_TIMEOUT 시간 동안 이벤트 없이 유휴 상태인 경우
        emitter.onError(e -> emitters.remove(emitter));       // 네트워크 오류 등으로 전송 실패한 경우

        return emitter;
    }

    /**
     * 초기 데이터 전송이 완료된 emitter를 브로드캐스트 리스트에 등록한다.
     */
    public void activate(SseEmitter emitter) {
        emitters.add(emitter);
        log.info("SSE 클라이언트 연결. 현재 연결 수: {}", emitters.size());
    }

    /**
     * 모든 SSE 클라이언트에 데이터를 브로드캐스트한다.
     */
    public void broadcast(String eventName, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                emitters.remove(emitter); // 전송 실패한 emitter 즉시 제거
            }
        }
    }

    /**
     * 특정 emitter에 데이터를 전송한다. (초기 데이터 전송용)
     */
    public void sendTo(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("SSE 초기 데이터 전송 실패", e);
        }
    }
}
