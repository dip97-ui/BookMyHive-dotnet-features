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
        List<Event> synced = tmdbSyncService.syncUpcomingMovies(limit, apiKey);
        List<EventDto.Response> dtos = synced.stream().map(EntityMapper::toEvent).toList();
        return ResponseEntity.ok(Map.of(
                "message", "Successfully synchronized " + synced.size() + " upcoming movies from TMDB",
                "count", synced.size(),
                "movies", dtos
        ));
    }
}
