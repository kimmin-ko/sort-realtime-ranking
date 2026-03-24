package study.min.sortrealtimeranking.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L; // 1시간

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 새 SSE 연결을 등록하고 SseEmitter를 반환한다.
     */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter)); // 클라이언트가 정상적으로 연결을 종료한 경우
        emitter.onTimeout(() -> emitters.remove(emitter)); // SSE_TIMEOUT 시간 동안 이벤트 없이 유휴 상태인 경우
        emitter.onError(e -> emitters.remove(emitter)); // 네트워크 오류 등으로 전송 실패한 경우

        log.info("SSE 클라이언트 연결. 현재 연결 수: {}", emitters.size());
        return emitter;
    }

    /**
     * 모든 SSE 클라이언트에 데이터를 브로드캐스트한다.
     */
    public void broadcast(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
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
            emitters.remove(emitter);
        }
    }
}
