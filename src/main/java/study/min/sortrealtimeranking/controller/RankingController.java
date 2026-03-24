package study.min.sortrealtimeranking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import study.min.sortrealtimeranking.dto.*;
import study.min.sortrealtimeranking.service.RankingService;
import study.min.sortrealtimeranking.sse.SseEmitterManager;

import java.util.UUID;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final SseEmitterManager sseEmitterManager;

    @PostMapping("/score")
    public ResponseEntity<UserRankingResponse> increaseScore(@Valid @RequestBody ScoreUpdateRequest request) {
        UserRankingResponse response = rankingService.increaseScore(
                request.userId(), request.nickname(), request.score()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top")
    public ResponseEntity<RankingListResponse> getTopRankings(@RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(rankingService.getTopRankings(size));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserRankingResponse> getUserRanking(@PathVariable UUID userId) {
        UserRankingResponse response = rankingService.getUserRanking(userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/nearby")
    public ResponseEntity<NearbyRankingResponse> getNearbyRankings(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int range) {
        NearbyRankingResponse response = rankingService.getNearbyRankings(userId, range);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRankings() {
        SseEmitter emitter = sseEmitterManager.create();

        // 초기 데이터를 먼저 전송한 후 브로드캐스트 리스트에 등록 (순서 보장)
        RankingListResponse initialData = rankingService.getTopRankings(100);
        sseEmitterManager.sendTo(emitter, "ranking", initialData);
        sseEmitterManager.activate(emitter);

        return emitter;
    }
}
