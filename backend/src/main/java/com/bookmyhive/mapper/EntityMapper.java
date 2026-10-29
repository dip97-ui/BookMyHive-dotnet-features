package com.bookmyhive.mapper;

import com.bookmyhive.dto.*;
import com.bookmyhive.entity.*;

import java.util.Comparator;
import java.util.List;
import java.util.Collection;

/** Converts entities to response DTOs. Call inside a transaction (lazy associations). */
public final class EntityMapper {

    /** Orders seat labels naturally: A1, A2, A10, B1 ... */
    private static final Comparator<String> SEAT_ORDER = Comparator
            .comparing((String s) -> s.charAt(0))
            .thenComparingInt(s -> Integer.parseInt(s.substring(1)));

    private EntityMapper() {
    }

    public static GenreDto.Response toGenre(Genre g) {
        return new GenreDto.Response(g.getId(), g.getName());
    }

    public static VenueDto.Response toVenue(Venue v) {
        return new VenueDto.Response(v.getId(), v.getName(), v.getCity(), v.getAddress());
    }

    public static EventDto.Summary toSummary(Event e) {
        return new EventDto.Summary(e.getId(), e.getTitle(), e.getType(), e.getPosterUrl(),
                e.getLanguage(), e.getDurationMinutes());
    }

    public static EventDto.Response toEvent(Event e) {
        List<GenreDto.Response> genres = e.getGenres().stream()
                .sorted(Comparator.comparing(Genre::getName))
                .map(EntityMapper::toGenre)
                .toList();
        return new EventDto.Response(e.getId(), e.getTitle(), e.getType(), e.getDescription(), e.getLanguage(),
                e.getDurationMinutes(), e.getCertification(), e.getRating(), e.getPosterUrl(),
                e.getReleaseDate(), e.isActive(), genres,
                e.getTmdbId(), e.getBackdropUrl(), e.getCast(), e.getDirector());
    }

    public static ShowDto.Response toShow(Show s, Collection<String> bookedSeats) {
        List<String> booked = bookedSeats.stream().sorted(SEAT_ORDER).toList();
        return new ShowDto.Response(s.getId(), toSummary(s.getEvent()), toVenue(s.getVenue()), s.getShowTime(),
                s.getPrice(), s.getSeatRows(), s.getSeatsPerRow(), s.getTotalSeats(),
                s.getTotalSeats() - booked.size(), booked);
    }

    public static BookingDto.Response toBooking(Booking b) {
        Show s = b.getShow();
        Payment p = b.getPayment();
        return new BookingDto.Response(b.getId(), b.getReference(), b.getStatus(),
                b.getSeats().stream().sorted(SEAT_ORDER).toList(),
                b.getTotalAmount(), b.getBookedAt(), s.getId(), s.getShowTime(), toSummary(s.getEvent()),
                s.getVenue().getName(), s.getVenue().getCity(),
                p == null ? null : p.getMethod(), p == null ? null : p.getStatus(),
                b.getUser().getName(), b.getUser().getEmail());
    }
}
