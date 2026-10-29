package com.bookmyhive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class GenreDto {
    private GenreDto() {
    }

    public record Request(@NotBlank(message = "Genre name is required") @Size(max = 60) String name) {
    }

    public record Response(Long id, String name) {
    }
}
