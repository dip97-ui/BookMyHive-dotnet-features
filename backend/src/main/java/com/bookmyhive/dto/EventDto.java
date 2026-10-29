package com.bookmyhive.dto;

import com.bookmyhive.entity.EventType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class EventDto {
    private EventDto() {
    }

    public record Request(
            @NotBlank(message = "Title is required") @Size(max = 200) String title,
            @NotNull(message = "Type must be MOVIE or PLAY") EventType type,
            @NotBlank(message = "Description is required") @Size(max = 2000) String description,
            @NotBlank(message = "Language is required") @Size(max = 60) String language,
            @NotNull(message = "Duration is required") @Min(value = 1, message = "Duration must be at least 1 minute")
            @Max(600) Integer durationMinutes,
            @Size(max = 10) String certification,
            @DecimalMin(value = "0.0", message = "Rating must be between 0 and 10")
            @DecimalMax(value = "10.0", message = "Rating must be between 0 and 10") Double rating,
            @Size(max = 500) String posterUrl,
            LocalDate releaseDate,
            Set<Long> genreIds,
            Boolean active,
            Long tmdbId,
            String backdropUrl,
            String cast,
            String director) {
    }

    public record Response(Long id, String title, EventType type, String description, String language,
                           Integer durationMinutes, String certification, Double rating, String posterUrl,
                           LocalDate releaseDate, boolean active, List<GenreDto.Response> genres,
                           Long tmdbId, String backdropUrl, String cast, String director) {
    }

    /** Small projection embedded inside show and booking responses. */
    public record Summary(Long id, String title, EventType type, String posterUrl, String language,
                          Integer durationMinutes) {
    }
}
