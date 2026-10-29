package com.bookmyhive.controller;

import com.bookmyhive.dto.VenueDto;
import com.bookmyhive.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public List<VenueDto.Response> list() {
        return venueService.findAll();
    }

    @GetMapping("/{id}")
    public VenueDto.Response getOne(@PathVariable Long id) {
        return venueService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VenueDto.Response> create(@Valid @RequestBody VenueDto.Request req) {
        VenueDto.Response created = venueService.create(req);
        return ResponseEntity.created(URI.create("/api/venues/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VenueDto.Response update(@PathVariable Long id, @Valid @RequestBody VenueDto.Request req) {
        return venueService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        venueService.delete(id);
    }
}
