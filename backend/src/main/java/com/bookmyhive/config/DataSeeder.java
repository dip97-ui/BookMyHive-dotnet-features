package com.bookmyhive.config;

import com.bookmyhive.entity.*;
import com.bookmyhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Fills an empty database with demo accounts, venues, events and shows.
 * Demo logins: admin@bookmyhive.com / Admin@123 and user@bookmyhive.com / User@123
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final List<LocalTime> SLOTS = List.of(LocalTime.of(11, 0), LocalTime.of(15, 30), LocalTime.of(20, 0));

    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final ShowRepository showRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.bookmyhive.service.TmdbSyncService tmdbSyncService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            tmdbSyncService.syncUpcomingMoviesIfConfigured();
            return;
        }
        createUser("Hive Admin", "admin@bookmyhive.com", "Admin@123", Role.ADMIN);
        createUser("Demo User", "user@bookmyhive.com", "User@123", Role.USER);

        Map<String, Genre> genres = new HashMap<>();
        for (String name : List.of("Drama", "Comedy", "Thriller", "Romance", "Sci-Fi", "Crime", "Adventure", "Musical", "Action", "Horror", "Fantasy")) {
            genres.put(name, genreRepository.save(new Genre(name)));
        }

        List<Venue> venues = venueRepository.saveAll(List.of(
                new Venue("Hive Cinemas Andheri", "Mumbai", "Link Road, Andheri West"),
                new Venue("Honeycomb Multiplex Powai", "Mumbai", "Hiranandani Gardens, Powai"),
                new Venue("Apiary Arts Centre", "Mumbai", "Nariman Point"),
                new Venue("Comb Square Cinemas", "Delhi", "Select Citywalk, Saket"),
                new Venue("Nectar Screens Indiranagar", "Bengaluru", "100 Feet Road, Indiranagar"),
                new Venue("Waggle Playhouse", "Pune", "Koregaon Park")));

        List<Event> plays = new ArrayList<>();
        plays.add(event("The Merchant of Bandra", EventType.PLAY, "English", 110, "U", 8.6, -30,
                "A sharp new staging of a classic about debt, loyalty and a loan with unusual terms, set in 1990s Bombay.",
                genres, "Drama", "Comedy"));
        plays.add(event("Ghalib Ki Haveli", EventType.PLAY, "Hindi", 95, "U", 9.0, -40,
                "An evening of verse and music inside the crumbling mansion of a poet who refuses to stop writing.",
                genres, "Drama", "Musical"));
        plays.add(event("Queen Bee Cabaret", EventType.PLAY, "English", 100, "UA", 7.8, -7,
                "A glittering cabaret revue where the queen of the hive hosts one last, very funny, coronation.",
                genres, "Musical", "Comedy"));
        eventRepository.saveAll(plays);
        createShows(plays, venues);

        tmdbSyncService.syncUpcomingMovies(10, null);
        log.info("Seeded demo data: venues, plays and TMDB upcoming movies. Admin login: admin@bookmyhive.com / Admin@123");
    }

    private void createUser(String name, String email, String rawPassword, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        userRepository.save(u);
    }

    private Event event(String title, EventType type, String language, int minutes, String cert, double rating,
                        int releaseOffsetDays, String description, Map<String, Genre> genres, String... genreNames) {
        Event e = new Event();
        e.setTitle(title);
        e.setType(type);
        e.setLanguage(language);
        e.setDurationMinutes(minutes);
        e.setCertification(cert);
        e.setRating(rating);
        e.setReleaseDate(LocalDate.now().plusDays(releaseOffsetDays));
        e.setDescription(description);
        for (String g : genreNames) {
            e.getGenres().add(genres.get(g));
        }
        return e;
    }

    /** Each event plays at one Mumbai venue and one venue in another city for the next 6 days. */
    private void createShows(List<Event> events, List<Venue> venues) {
        LocalDateTime earliest = LocalDateTime.now().plusMinutes(30);
        List<Show> shows = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            boolean play = event.getType() == EventType.PLAY;
            List<Venue> chosen = List.of(venues.get(i % 3), venues.get(3 + i % 3));
            for (Venue venue : chosen) {
                for (int day = 0; day < 6; day++) {
                    for (LocalTime slot : SLOTS) {
                        LocalDateTime when = LocalDate.now().plusDays(day).atTime(slot);
                        if (when.isBefore(earliest)) {
                            continue;
                        }
                        Show s = new Show();
                        s.setEvent(event);
                        s.setVenue(venue);
                        s.setShowTime(when);
                        s.setPrice(BigDecimal.valueOf(play ? 600 + (i % 3) * 150 : 180 + (i % 4) * 40));
                        s.setSeatRows(play ? 6 : 8);
                        s.setSeatsPerRow(play ? 10 : 12);
                        shows.add(s);
                    }
                }
            }
        }
        showRepository.saveAll(shows);
    }
}
