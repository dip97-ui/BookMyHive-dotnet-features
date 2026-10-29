package com.bookmyhive.dto;

import com.bookmyhive.entity.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class BookingDto {
    private BookingDto() {
    }

    public record Request(
            @NotNull(message = "Show is required") Long showId,
            @NotEmpty(message = "Select at least one seat")
            @Size(max = 10, message = "You can book at most 10 seats at a time") Set<@NotBlank String> seats,
            @Size(max = 30) String paymentMethod) {
    }

    public record Response(Long id, String reference, BookingStatus status, List<String> seats,
                           BigDecimal totalAmount, LocalDateTime bookedAt, Long showId, LocalDateTime showTime,
                           EventDto.Summary event, String venueName, String city,
                           String paymentMethod, String paymentStatus, String customerName, String customerEmail) {
    }
}
