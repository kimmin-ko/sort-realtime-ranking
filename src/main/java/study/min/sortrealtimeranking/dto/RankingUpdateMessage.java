package study.min.sortrealtimeranking.dto;

import java.util.UUID;

public record RankingUpdateMessage(UUID userId, String nickname, long score, long rank) {
}
