package com.bookmyhive.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ShowDto {
    private ShowDto() {
    }

    public record Request(
            @NotNull(message = "Event is required") Long eventId,
            @NotNull(message = "Venue is required") Long venueId,
            @NotNull(message = "Show time is required") @Future(message = "Show time must be in the future") LocalDateTime showTime,
            @NotNull(message = "Price is required") @DecimalMin(value = "1.0", message = "Price must be at least 1") BigDecimal price,
            @Min(value = 1, message = "At least 1 row") @Max(value = 26, message = "At most 26 rows") int seatRows,
            @Min(value = 1, message = "At least 1 seat per row") @Max(value = 30, message = "At most 30 seats per row") int seatsPerRow) {
    }

    public record Response(Long id, EventDto.Summary event, VenueDto.Response venue, LocalDateTime showTime,
                           BigDecimal price, int seatRows, int seatsPerRow, int totalSeats,
                           int availableSeats, List<String> bookedSeats) {
    }
}
