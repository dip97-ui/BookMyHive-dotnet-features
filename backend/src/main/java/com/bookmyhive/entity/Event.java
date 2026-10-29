package com.bookmyhive.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A movie or a play that can be scheduled at venues. */
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 60)
    private String language;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(length = 10)
    private String certification;

    private Double rating;

    @Column(length = 500)
    private String posterUrl;

    private LocalDate releaseDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "tmdb_id")
    private Long tmdbId;

    @Column(length = 500)
    private String backdropUrl;

    @Column(name = "cast_members", length = 1000)
    private String cast;

    @Column(length = 200)
    private String director;

    /** Many-to-Many: an event has several genres and a genre belongs to several events. */
    @ManyToMany
    @JoinTable(name = "event_genres",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    /** One-to-Many: an event is screened/performed in many shows. */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Show> shows = new ArrayList<>();
}
