package com.bookmyhive.service;

import com.bookmyhive.dto.TmdbDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.util.function.Consumer;

@Service
public class TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    private final RestClient restClient;

    @Value("${tmdb.api.key:}")
    private String apiKey;

    @Value("${tmdb.region:IN}")
    private String region;

    @Value("${tmdb.image.base-url:https://image.tmdb.org/t/p}")
    private String imageBaseUrl;

    public TmdbService(RestClient.Builder restClientBuilder,
                       @Value("${tmdb.api.url:https://api.themoviedb.org/3}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public String resolveKey(String keyOverride) {
        if (keyOverride != null && !keyOverride.isBlank()) {
            return keyOverride.trim();
        }
        return apiKey != null ? apiKey.trim() : "";
    }

    public boolean isConfigured() {
        String key = resolveKey(null);
        return !key.isBlank() && !key.equalsIgnoreCase("YOUR_TMDB_API_KEY");
    }

    public String getRegion() {
        return region;
    }

    public String buildPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return imageBaseUrl + "/w500" + (posterPath.startsWith("/") ? posterPath : "/" + posterPath);
    }

    public String buildBackdropUrl(String backdropPath) {
        if (backdropPath == null || backdropPath.isBlank()) {
            return null;
        }
        return imageBaseUrl + "/original" + (backdropPath.startsWith("/") ? backdropPath : "/" + backdropPath);
    }

    public TmdbDto.UpcomingResponse fetchUpcomingMovies(int page, String keyOverride) {
        String key = resolveKey(keyOverride);
        if (key.isBlank()) {
            return null;
        }
        try {
            return prepareRequest("/movie/upcoming", key, ub -> ub
                    .queryParam("region", region)
                    .queryParam("page", page))
                    .retrieve()
                    .body(TmdbDto.UpcomingResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch upcoming movies from TMDB: {}", e.getMessage());
            return null;
        }
    }

    public TmdbDto.UpcomingResponse fetchNowPlayingMovies(int page, String keyOverride) {
        String key = resolveKey(keyOverride);
        if (key.isBlank()) {
            return null;
        }
        try {
            return prepareRequest("/movie/now_playing", key, ub -> ub
                    .queryParam("region", region)
                    .queryParam("page", page))
                    .retrieve()
                    .body(TmdbDto.UpcomingResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch now playing movies from TMDB: {}", e.getMessage());
            return null;
        }
    }

    public TmdbDto.MovieDetail fetchMovieDetails(Long tmdbId, String keyOverride) {
        String key = resolveKey(keyOverride);
        if (key.isBlank() || tmdbId == null) {
            return null;
        }
        try {
            return prepareRequest("/movie/" + tmdbId, key, ub -> ub
                    .queryParam("append_to_response", "credits"))
                    .retrieve()
                    .body(TmdbDto.MovieDetail.class);
        } catch (Exception e) {
            log.error("Failed to fetch details for TMDB movie ID {}: {}", tmdbId, e.getMessage());
            return null;
        }
    }

    private RestClient.RequestHeadersSpec<?> prepareRequest(String path, String key, Consumer<UriBuilder> uriCustomizer) {
        boolean isBearer = key.startsWith("eyJ") || key.length() > 60;
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (!isBearer) {
                        uriBuilder.queryParam("api_key", key);
                    }
                    if (uriCustomizer != null) {
                        uriCustomizer.accept(uriBuilder);
                    }
                    return uriBuilder.build();
                })
                .headers(headers -> {
                    headers.set("Accept", "application/json");
                    if (isBearer) {
                        headers.set("Authorization", "Bearer " + key);
                    }
                });
    }

    public static String mapLanguage(String iso) {
        if (iso == null || iso.isBlank()) {
            return "Hindi";
        }
        return switch (iso.toLowerCase().trim()) {
            case "hi" -> "Hindi";
            case "en" -> "English";
            case "te" -> "Telugu";
            case "ta" -> "Tamil";
            case "ml" -> "Malayalam";
            case "mr" -> "Marathi";
            case "kn" -> "Kannada";
            case "pa" -> "Punjabi";
            case "bn" -> "Bengali";
            case "gu" -> "Gujarati";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "es" -> "Spanish";
            case "fr" -> "French";
            default -> Character.toUpperCase(iso.charAt(0)) + iso.substring(1).toLowerCase();
        };
    }

    public static String mapGenreId(int id) {
        return switch (id) {
            case 28 -> "Action";
            case 12 -> "Adventure";
            case 16 -> "Animation";
            case 35 -> "Comedy";
            case 80 -> "Crime";
            case 99 -> "Documentary";
            case 18 -> "Drama";
            case 10751 -> "Family";
            case 14 -> "Fantasy";
            case 36 -> "History";
            case 27 -> "Horror";
            case 10402 -> "Musical";
            case 9648 -> "Mystery";
            case 10749 -> "Romance";
            case 878 -> "Sci-Fi";
            case 53 -> "Thriller";
            case 10752 -> "War";
            default -> "Drama";
        };
    }
}
