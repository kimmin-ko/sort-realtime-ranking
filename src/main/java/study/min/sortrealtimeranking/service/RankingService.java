package study.min.sortrealtimeranking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import study.min.sortrealtimeranking.dto.NearbyRankingResponse;
import study.min.sortrealtimeranking.dto.RankingListResponse;
import study.min.sortrealtimeranking.dto.UserRankingResponse;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final String SCORE_KEY = "ranking:score";
    private static final String NICKNAME_KEY = "ranking:nickname";

    private final StringRedisTemplate redisTemplate;

    // ==================== Public API ====================

    /**
     * 사용자의 score를 증가시키고 현재 랭킹 정보를 반환한다.
     * Pipeline으로 ZINCRBY + HSET + ZREVRANK를 1회 RTT로 처리한다.
     */
    public UserRankingResponse increaseScore(UUID userId, String nickname, int score) {
        byte[] userIdBytes = toBytes(userId);
        byte[] scoreKeyBytes = toBytes(SCORE_KEY);
        byte[] nicknameKeyBytes = toBytes(NICKNAME_KEY);

        List<Object> results = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            connection.zSetCommands().zIncrBy(scoreKeyBytes, score, userIdBytes);             // score 원자적 증가 (없으면 생성)
            connection.hashCommands().hSet(nicknameKeyBytes, userIdBytes, toBytes(nickname)); // nickname 저장/갱신
            connection.zSetCommands().zRevRank(scoreKeyBytes, userIdBytes);                   // 현재 순위 조회 (내림차순)
            return null;
        });

        long totalScore = ((Double) results.get(0)).longValue();
        long rank = (Long) results.get(2);

        return new UserRankingResponse(userId, nickname, totalScore, rank + 1);
    }

    /**
     * 상위 N명의 랭킹을 조회한다.
     */
    public RankingListResponse getTopRankings(int size) {
        Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(SCORE_KEY, 0, (long) size - 1);

        if (tuples == null || tuples.isEmpty()) {
            return new RankingListResponse(List.of());
        }

        return new RankingListResponse(toRankingList(tuples, 1));
    }

    /**
     * 특정 사용자의 랭킹을 조회한다.
     * Pipeline으로 ZSCORE + ZREVRANK + HGET을 1회 RTT로 처리한다.
     */
    public UserRankingResponse getUserRanking(UUID userId) {
        byte[] userIdBytes = toBytes(userId);
        byte[] scoreKeyBytes = toBytes(SCORE_KEY);
        byte[] nicknameKeyBytes = toBytes(NICKNAME_KEY);

        List<Object> results = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            connection.zSetCommands().zScore(scoreKeyBytes, userIdBytes);     // 사용자의 현재 score 조회
            connection.zSetCommands().zRevRank(scoreKeyBytes, userIdBytes);   // 내림차순 순위 조회
            connection.hashCommands().hGet(nicknameKeyBytes, userIdBytes);    // nickname 조회
            return null;
        });

        Double score = (Double) results.get(0);
        Long rank = (Long) results.get(1);
        if (score == null || rank == null) {
            return null;
        }

        String nickname = decodeNickname((byte[]) results.get(2));
        return new UserRankingResponse(userId, nickname, score.longValue(), rank + 1);
    }

    /**
     * 특정 사용자 기준 앞뒤 range명의 주변 랭킹을 조회한다.
     */
    public NearbyRankingResponse getNearbyRankings(UUID userId, int range) {
        Long rank = redisTemplate.opsForZSet().reverseRank(SCORE_KEY, userId.toString());
        if (rank == null) {
            return null;
        }

        long start = Math.max(0, rank - range);
        long end = rank + range;

        Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(SCORE_KEY, start, end);

        if (tuples == null || tuples.isEmpty()) {
            return null;
        }

        List<UserRankingResponse> rankings = toRankingList(tuples, start + 1);
        UserRankingResponse targetUser = rankings.stream()
                .filter(r -> r.userId().equals(userId))
                .findFirst()
                .orElse(null);

        return new NearbyRankingResponse(targetUser, rankings);
    }

    // ==================== Private Helpers ====================

    /**
     * Sorted Set 조회 결과를 UserRankingResponse 리스트로 변환한다.
     * nickname은 Redis Hash에서 HMGET으로 일괄 조회한다.
     */
    private List<UserRankingResponse> toRankingList(Set<TypedTuple<String>> tuples, long startRank) {
        List<String> userIds = tuples.stream()
                .map(TypedTuple::getValue)
                .toList();

        List<Object> nicknames = redisTemplate.opsForHash()
                .multiGet(NICKNAME_KEY, new ArrayList<>(userIds));

        List<UserRankingResponse> rankings = new ArrayList<>();
        long currentRank = startRank;
        int index = 0;

        for (TypedTuple<String> tuple : tuples) {
            rankings.add(new UserRankingResponse(
                    UUID.fromString(tuple.getValue()),
                    (String) nicknames.get(index),
                    scoreOf(tuple),
                    currentRank++
            ));
            index++;
        }

        return rankings;
    }

    private static long scoreOf(TypedTuple<String> tuple) {
        Double score = tuple.getScore();
        return score != null ? score.longValue() : 0L;
    }

    private static String decodeNickname(byte[] bytes) {
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    private static byte[] toBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] toBytes(UUID uuid) {
        return uuid.toString().getBytes(StandardCharsets.UTF_8);
    }
}
