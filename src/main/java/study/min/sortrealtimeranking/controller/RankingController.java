package study.min.sortrealtimeranking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import study.min.sortrealtimeranking.dto.*;
import study.min.sortrealtimeranking.service.RankingService;

import java.util.UUID;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

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
}
