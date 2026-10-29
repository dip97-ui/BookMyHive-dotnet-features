package com.bookmyhive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class VenueDto {
    private VenueDto() {
    }

    public record Request(
            @NotBlank(message = "Venue name is required") @Size(max = 150) String name,
            @NotBlank(message = "City is required") @Size(max = 80) String city,
            @NotBlank(message = "Address is required") @Size(max = 300) String address) {
    }

    public record Response(Long id, String name, String city, String address) {
    }
}
