package com.bookmyhive.dto;

import java.math.BigDecimal;

public record StatsDto(long events, long venues, long shows, long bookings, long users, BigDecimal revenue) {
}
