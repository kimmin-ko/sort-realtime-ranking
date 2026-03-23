package study.min.sortrealtimeranking.dto;

import java.util.List;

public record RankingListResponse(
        List<UserRankingResponse> rankings
) {
}
