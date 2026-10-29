package com.bookmyhive.controller;

import com.bookmyhive.dto.BookingDto;
import com.bookmyhive.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** Any logged-in user (USER or ADMIN) can book and manage their own bookings. */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto.Response> create(@Valid @RequestBody BookingDto.Request req,
                                                      Authentication auth) {
        BookingDto.Response created = bookingService.create(auth.getName(), req);
        return ResponseEntity.created(URI.create("/api/bookings/" + created.id())).body(created);
    }

    @GetMapping("/my")
    public List<BookingDto.Response> mine(Authentication auth) {
        return bookingService.findMine(auth.getName());
    }

    @PatchMapping("/{id}/cancel")
    public BookingDto.Response cancel(@PathVariable Long id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return bookingService.cancel(id, auth.getName(), isAdmin);
    }
}
