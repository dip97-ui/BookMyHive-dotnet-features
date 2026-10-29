package com.bookmyhive.controller;

import com.bookmyhive.dto.ShowDto;
import com.bookmyhive.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    /** Public: includes the list of already booked seats for the seat map. */
    @GetMapping("/{id}")
    public ShowDto.Response getOne(@PathVariable Long id) {
        return showService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDto.Response> create(@Valid @RequestBody ShowDto.Request req) {
        ShowDto.Response created = showService.create(req);
        return ResponseEntity.created(URI.create("/api/shows/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ShowDto.Response update(@PathVariable Long id, @Valid @RequestBody ShowDto.Request req) {
        return showService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        showService.delete(id);
    }
}
