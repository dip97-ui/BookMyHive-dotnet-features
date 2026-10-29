package com.bookmyhive.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One screening/performance of an event at a venue at a given time. */
@Entity
@Table(name = "shows")
@Data
@NoArgsConstructor
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(nullable = false)
    private LocalDateTime showTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Seat layout: rows are labelled A, B, C ... and seats are numbered 1..seatsPerRow. */
    @Column(nullable = false)
    private int seatRows;

    @Column(nullable = false)
    private int seatsPerRow;

    @OneToMany(mappedBy = "show")
    private List<Booking> bookings = new ArrayList<>();

    public int getTotalSeats() {
        return seatRows * seatsPerRow;
    }
}
