package study.min.sortrealtimeranking.dto;

import java.util.UUID;

public record UserRankingResponse(
        UUID userId,
        String nickname,
        long score,
        long rank
) {
}
