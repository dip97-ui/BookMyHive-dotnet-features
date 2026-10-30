package com.bookmyhive.controller;

import com.bookmyhive.dto.EventDto;
import com.bookmyhive.dto.TmdbDto;
import com.bookmyhive.entity.Event;
import com.bookmyhive.mapper.EntityMapper;
import com.bookmyhive.service.TmdbService;
import com.bookmyhive.service.TmdbSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
public class TmdbController {

    private final TmdbService tmdbService;
    private final TmdbSyncService tmdbSyncService;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "configured", tmdbService.isConfigured(),
                "region", tmdbService.getRegion()
        );
    }

    @GetMapping("/upcoming")
    public TmdbDto.UpcomingResponse upcoming(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String apiKey) {
        return tmdbService.fetchUpcomingMovies(page, apiKey);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sync(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String apiKey) {
        TmdbSyncService.SyncResult result = tmdbSyncService.syncUpcomingMovies(limit, apiKey);
        List<EventDto.Response> dtos = result.events().stream().map(EntityMapper::toEvent).toList();
        String message;
        if (result.added() > 0) {
            message = "Sync complete: " + result.added() + " new movie(s) added"
                    + (result.updated() > 0 ? ", " + result.updated() + " existing refreshed" : "");
        } else if (result.updated() > 0) {
            message = "Sync complete: no new movies to add; " + result.updated() + " existing movie(s) refreshed";
        } else {
            message = "No new movies to add - your catalogue already has every title TMDB lists for your region right now";
        }
        return ResponseEntity.ok(Map.of(
                "message", message,
                "count", result.added() + result.updated(),
                "added", result.added(),
                "updated", result.updated(),
                "movies", dtos
        ));
    }
}
