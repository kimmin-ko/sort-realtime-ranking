package study.min.sortrealtimeranking.dto;

import java.util.List;

public record NearbyRankingResponse(
        UserRankingResponse targetUser,
        List<UserRankingResponse> rankings
) {
}
