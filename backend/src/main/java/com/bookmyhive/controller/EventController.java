package com.bookmyhive.controller;

import com.bookmyhive.dto.EventDto;
import com.bookmyhive.dto.ShowDto;
import com.bookmyhive.entity.EventType;
import com.bookmyhive.service.EventService;
import com.bookmyhive.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final ShowService showService;

    /** Public catalogue. Optional filters: type, q (title), genre, language. */
    @GetMapping
    public List<EventDto.Response> search(@RequestParam(required = false) EventType type,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) String genre,
                                          @RequestParam(required = false) String language) {
        return eventService.search(type, q, genre, language);
    }

    @GetMapping("/{id}")
    public EventDto.Response getOne(@PathVariable Long id) {
        return eventService.findById(id);
    }

    /** Upcoming shows of an event, optionally limited to a city. */
    @GetMapping("/{id}/shows")
    public List<ShowDto.Response> shows(@PathVariable Long id, @RequestParam(required = false) String city) {
        return showService.findUpcomingByEvent(id, city);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventDto.Response> create(@Valid @RequestBody EventDto.Request req) {
        EventDto.Response created = eventService.create(req);
        return ResponseEntity.created(URI.create("/api/events/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventDto.Response update(@PathVariable Long id, @Valid @RequestBody EventDto.Request req) {
        return eventService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }
}
