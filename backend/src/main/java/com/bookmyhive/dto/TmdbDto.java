package com.bookmyhive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class TmdbDto {

    private TmdbDto() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovieSummary(
            Long id,
            String title,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("vote_average") Double voteAverage,
            @JsonProperty("original_language") String originalLanguage,
            @JsonProperty("genre_ids") List<Integer> genreIds
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpcomingResponse(
            Integer page,
            List<MovieSummary> results,
            @JsonProperty("total_results") Integer totalResults,
            @JsonProperty("total_pages") Integer totalPages
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenreItem(
            Integer id,
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CastItem(
            Long id,
            String name,
            String character,
            @JsonProperty("profile_path") String profilePath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CrewItem(
            Long id,
            String name,
            String job,
            String department
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Credits(
            List<CastItem> cast,
            List<CrewItem> crew
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovieDetail(
            Long id,
            String title,
            String overview,
            Integer runtime,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("vote_average") Double voteAverage,
            @JsonProperty("original_language") String originalLanguage,
            List<GenreItem> genres,
            Credits credits
    ) {
    }
}
