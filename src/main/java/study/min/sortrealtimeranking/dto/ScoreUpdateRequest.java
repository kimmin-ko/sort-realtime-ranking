package study.min.sortrealtimeranking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ScoreUpdateRequest(
        @NotNull UUID userId,
        @NotBlank String nickname,
        @Positive int score
) {
}
