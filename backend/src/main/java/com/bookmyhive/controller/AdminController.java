package com.bookmyhive.controller;

import com.bookmyhive.dto.BookingDto;
import com.bookmyhive.dto.EventDto;
import com.bookmyhive.dto.ShowDto;
import com.bookmyhive.dto.StatsDto;
import com.bookmyhive.service.AdminService;
import com.bookmyhive.service.BookingService;
import com.bookmyhive.service.EventService;
import com.bookmyhive.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only back-office views, restricted to the ADMIN role. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final EventService eventService;
    private final ShowService showService;
    private final BookingService bookingService;

    @GetMapping("/stats")
    public StatsDto stats() {
        return adminService.stats();
    }

    /** Includes inactive events, unlike the public catalogue. */
    @GetMapping("/events")
    public List<EventDto.Response> events() {
        return eventService.findAllForAdmin();
    }

    @GetMapping("/shows")
    public List<ShowDto.Response> shows() {
        return showService.findAllForAdmin();
    }

    @GetMapping("/bookings")
    public List<BookingDto.Response> bookings() {
        return bookingService.findAll();
    }
}
