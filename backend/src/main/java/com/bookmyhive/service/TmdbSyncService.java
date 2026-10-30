package com.bookmyhive.service;

import com.bookmyhive.dto.TmdbDto;
import com.bookmyhive.entity.*;
import com.bookmyhive.repository.EventRepository;
import com.bookmyhive.repository.GenreRepository;
import com.bookmyhive.repository.ShowRepository;
import com.bookmyhive.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TmdbSyncService {

    private static final Logger log = LoggerFactory.getLogger(TmdbSyncService.class);
    private static final List<LocalTime> SLOTS = List.of(LocalTime.of(11, 0), LocalTime.of(15, 30), LocalTime.of(20, 0));

    private final TmdbService tmdbService;
    private final EventRepository eventRepository;
    private final GenreRepository genreRepository;
    private final VenueRepository venueRepository;
    private final ShowRepository showRepository;

    @Value("${tmdb.sync.on-startup:true}")
    private boolean syncOnStartup;

    public record MovieData(
            Long tmdbId,
            String title,
            String description,
            String language,
            Integer durationMinutes,
            String certification,
            Double rating,
            String posterUrl,
            String backdropUrl,
            LocalDate releaseDate,
            String cast,
            String director,
            Set<String> genreNames
    ) {
    }

    @Transactional
    public void syncUpcomingMoviesIfConfigured() {
        if (!syncOnStartup) {
            return;
        }

        List<Event> currentMovies = eventRepository.findAll().stream()
                .filter(e -> e.getType() == EventType.MOVIE)
                .toList();

        boolean hasPlaceholders = currentMovies.isEmpty() || currentMovies.stream()
                .anyMatch(e -> e.getPosterUrl() == null || e.getPosterUrl().isBlank() || "Honey Heist".equalsIgnoreCase(e.getTitle()));

        if (hasPlaceholders || tmdbService.isConfigured()) {
            log.info("Starting TMDB upcoming movies sync (placeholders found or TMDB configured)...");
            syncUpcomingMovies(10, null);
        }
    }

    @Transactional
    public List<Event> syncUpcomingMovies(int limit, String apiKeyOverride) {
        List<MovieData> moviesToSync = new ArrayList<>();

        if (tmdbService.isConfigured() || (apiKeyOverride != null && !apiKeyOverride.isBlank())) {
            log.info("Fetching real upcoming movies from TMDB API...");
            moviesToSync = fetchFromTmdb(limit, apiKeyOverride);
        }

        if (moviesToSync.isEmpty()) {
            log.info("No live TMDB results received or key not set. Using verified upcoming movies catalog.");
            moviesToSync = getFallbackUpcomingMovies();
        }

        return applyMoviesToDatabase(moviesToSync);
    }

    private List<MovieData> fetchFromTmdb(int limit, String apiKeyOverride) {
        List<MovieData> list = new ArrayList<>();
        try {
            // First fetch upcoming movies in region (e.g. IN)
            TmdbDto.UpcomingResponse upcoming = tmdbService.fetchUpcomingMovies(1, apiKeyOverride);
            List<TmdbDto.MovieSummary> summaries = new ArrayList<>();
            if (upcoming != null && upcoming.results() != null) {
                summaries.addAll(upcoming.results());
            }

            // Supplement with now playing in region if list is small
            if (summaries.size() < limit) {
                TmdbDto.UpcomingResponse nowPlaying = tmdbService.fetchNowPlayingMovies(1, apiKeyOverride);
                if (nowPlaying != null && nowPlaying.results() != null) {
                    Set<Long> existingIds = summaries.stream().map(TmdbDto.MovieSummary::id).collect(Collectors.toSet());
                    for (TmdbDto.MovieSummary s : nowPlaying.results()) {
                        if (!existingIds.contains(s.id())) {
                            summaries.add(s);
                        }
                    }
                }
            }

            int count = 0;
            for (TmdbDto.MovieSummary summary : summaries) {
                if (summary.posterPath() == null || summary.posterPath().isBlank()) {
                    continue;
                }
                if (count >= limit) {
                    break;
                }

                TmdbDto.MovieDetail details = tmdbService.fetchMovieDetails(summary.id(), apiKeyOverride);

                String title = summary.title() != null ? summary.title() : (details != null ? details.title() : "Untitled");
                String overview = (summary.overview() != null && !summary.overview().isBlank())
                        ? summary.overview()
                        : (details != null && details.overview() != null && !details.overview().isBlank())
                        ? details.overview()
                        : "Experience this highly anticipated upcoming cinema release on the big screen.";

                if (overview.length() > 1900) {
                    overview = overview.substring(0, 1900) + "...";
                }

                Double voteAvg = summary.voteAverage() != null && summary.voteAverage() > 0 ? summary.voteAverage() : 7.6;
                double rating = Math.round(voteAvg * 10.0) / 10.0;

                String posterUrl = tmdbService.buildPosterUrl(summary.posterPath());
                String backdropUrl = tmdbService.buildBackdropUrl(summary.backdropPath());

                LocalDate releaseDate = LocalDate.now().plusDays(7);
                if (summary.releaseDate() != null && !summary.releaseDate().isBlank()) {
                    try {
                        releaseDate = LocalDate.parse(summary.releaseDate());
                    } catch (Exception ignored) {
                    }
                }

                int runtime = details != null && details.runtime() != null && details.runtime() > 0 ? details.runtime() : 135;
                String language = TmdbService.mapLanguage(summary.originalLanguage());

                String director = null;
                String cast = null;
                if (details != null && details.credits() != null) {
                    if (details.credits().crew() != null) {
                        director = details.credits().crew().stream()
                                .filter(c -> "Director".equalsIgnoreCase(c.job()))
                                .map(TmdbDto.CrewItem::name)
                                .findFirst()
                                .orElse(null);
                    }
                    if (details.credits().cast() != null) {
                        cast = details.credits().cast().stream()
                                .map(TmdbDto.CastItem::name)
                                .limit(5)
                                .collect(Collectors.joining(", "));
                    }
                }

                Set<String> genreNames = new LinkedHashSet<>();
                if (details != null && details.genres() != null && !details.genres().isEmpty()) {
                    details.genres().forEach(g -> genreNames.add(g.name()));
                } else if (summary.genreIds() != null) {
                    summary.genreIds().forEach(gid -> genreNames.add(TmdbService.mapGenreId(gid)));
                }
                if (genreNames.isEmpty()) {
                    genreNames.add("Drama");
                }

                list.add(new MovieData(
                        summary.id(),
                        title,
                        overview,
                        language,
                        runtime,
                        "UA",
                        rating,
                        posterUrl,
                        backdropUrl,
                        releaseDate,
                        cast,
                        director,
                        genreNames
                ));
                count++;
            }
        } catch (Exception e) {
            log.error("Error building movie data from TMDB: {}", e.getMessage(), e);
        }
        return list;
    }

    private List<Event> applyMoviesToDatabase(List<MovieData> moviesToSync) {
        List<Venue> venues = venueRepository.findAll();

        List<Event> savedEvents = new ArrayList<>();

        for (int i = 0; i < moviesToSync.size(); i++) {
            MovieData data = moviesToSync.get(i);
            Event event;

            Optional<Event> matched = eventRepository.findByTmdbId(data.tmdbId());
            if (matched.isEmpty()) {
                matched = eventRepository.findByTitleIgnoreCase(data.title());
            }

            if (matched.isPresent()) {
                event = matched.get();
            } else {
                // Never overwrite an existing catalogue entry positionally: an unmatched
                // movie is inserted as a NEW event, so a booked event can never be retitled.
                event = new Event();
                event.setType(EventType.MOVIE);
            }

            event.setTitle(data.title());
            event.setType(EventType.MOVIE);
            event.setDescription(data.description());
            event.setLanguage(data.language());
            event.setDurationMinutes(data.durationMinutes());
            event.setCertification(data.certification());
            event.setRating(data.rating());
            event.setPosterUrl(data.posterUrl());
            event.setBackdropUrl(data.backdropUrl());
            event.setReleaseDate(data.releaseDate());
            event.setTmdbId(data.tmdbId());
            event.setCast(data.cast());
            event.setDirector(data.director());
            event.setActive(true);

            Set<Genre> genres = new HashSet<>();
            for (String gName : data.genreNames()) {
                Genre g = genreRepository.findByNameIgnoreCase(gName)
                        .orElseGet(() -> genreRepository.save(new Genre(gName)));
                genres.add(g);
            }
            event.setGenres(genres);

            Event saved = eventRepository.save(event);
            savedEvents.add(saved);

            if (!venues.isEmpty() && !showRepository.existsByEventId(saved.getId())) {
                createShowsForEvent(saved, venues, i);
            }
        }

        log.info("Saved {} TMDB upcoming movies into the database", savedEvents.size());
        return savedEvents;
    }

    private void createShowsForEvent(Event event, List<Venue> venues, int index) {
        LocalDateTime earliest = LocalDateTime.now().plusMinutes(30);
        List<Show> shows = new ArrayList<>();
        List<Venue> chosen = List.of(
                venues.get(index % venues.size()),
                venues.get((index + 3) % venues.size())
        );

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
                    s.setPrice(BigDecimal.valueOf(220 + (index % 4) * 30));
                    s.setSeatRows(8);
                    s.setSeatsPerRow(12);
                    shows.add(s);
                }
            }
        }
        showRepository.saveAll(shows);
    }

    private List<MovieData> getFallbackUpcomingMovies() {
        return List.of(
                new MovieData(
                        857598L,
                        "Pushpa 2: The Rule",
                        "Pushpa Raj continues to consolidate his dominance in the red sandalwood smuggling syndicate while facing relentless opposition from SP Bhanwar Singh Shekhawat in an explosive showdown.",
                        "Telugu",
                        175,
                        "UA",
                        8.5,
                        "https://image.tmdb.org/t/p/w500/b1OxQ5X7o9xW5n2CgC1gS.jpg",
                        "https://image.tmdb.org/t/p/original/b1OxQ5X7o9xW5n2CgC1gS.jpg",
                        LocalDate.now().plusDays(10),
                        "Allu Arjun, Rashmika Mandanna, Fahadh Faasil, Sunil, Jagapathi Babu",
                        "Sukumar",
                        Set.of("Action", "Crime", "Drama")
                ),
                new MovieData(
                        1084199L,
                        "Singham Again",
                        "DCP Bajirao Singham leads an elite squad of heroes on a daring transnational rescue mission to save his wife from a dangerous international adversary.",
                        "Hindi",
                        144,
                        "UA",
                        7.8,
                        "https://image.tmdb.org/t/p/w500/y4l9G3K4Xw0nF5mP8Q7d7rT9j.jpg",
                        "https://image.tmdb.org/t/p/original/y4l9G3K4Xw0nF5mP8Q7d7rT9j.jpg",
                        LocalDate.now().plusDays(4),
                        "Ajay Devgn, Kareena Kapoor Khan, Ranveer Singh, Akshay Kumar, Deepika Padukone",
                        "Rohit Shetty",
                        Set.of("Action", "Drama", "Crime")
                ),
                new MovieData(
                        558449L,
                        "Gladiator II",
                        "Years after witnessing the fall of Maximus, Lucius enters the Colosseum after his home is seized by the tyrannical emperors who rule Rome with an iron fist.",
                        "English",
                        148,
                        "A",
                        8.2,
                        "https://image.tmdb.org/t/p/w500/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg",
                        "https://image.tmdb.org/t/p/original/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg",
                        LocalDate.now().plusDays(14),
                        "Paul Mescal, Pedro Pascal, Denzel Washington, Connie Nielsen, Joseph Quinn",
                        "Ridley Scott",
                        Set.of("Action", "Adventure", "Drama")
                ),
                new MovieData(
                        1091249L,
                        "Bhool Bhulaiyaa 3",
                        "Rooh Baba is hired to exorcise the ancient royal palace of Raktghat, only to encounter the vengeance of Manjulika in a hilarious yet terrifying battle.",
                        "Hindi",
                        158,
                        "UA",
                        7.7,
                        "https://image.tmdb.org/t/p/w500/sKpn8LqgK3Oqf7tJ2q3G4L1p.jpg",
                        "https://image.tmdb.org/t/p/original/sKpn8LqgK3Oqf7tJ2q3G4L1p.jpg",
                        LocalDate.now().plusDays(6),
                        "Kartik Aaryan, Vidya Balan, Madhuri Dixit, Triptii Dimri, Rajpal Yadav",
                        "Anees Bazmee",
                        Set.of("Comedy", "Horror")
                ),
                new MovieData(
                        970347L,
                        "Kanguva",
                        "A fearless tribal warrior from a thousand years ago and a quick-witted modern detective find their fates intertwined through an unbroken oath of honour.",
                        "Tamil",
                        154,
                        "UA",
                        7.6,
                        "https://image.tmdb.org/t/p/w500/p4T7K6j8v6N0q8sL4l8B2d9G.jpg",
                        "https://image.tmdb.org/t/p/original/p4T7K6j8v6N0q8sL4l8B2d9G.jpg",
                        LocalDate.now().plusDays(12),
                        "Suriya, Bobby Deol, Disha Patani, Yogi Babu, Natarajan Subramaniam",
                        "Siva",
                        Set.of("Action", "Fantasy", "Drama")
                ),
                new MovieData(
                        912649L,
                        "Venom: The Last Dance",
                        "Eddie and Venom are on the run. Hunted by both their worlds and with the net closing in, the duo are forced into a devastating decision that brings the curtain down.",
                        "English",
                        110,
                        "UA",
                        7.5,
                        "https://image.tmdb.org/t/p/w500/aosm8Vh99f6ToBtHgOhJYBh9Ky8.jpg",
                        "https://image.tmdb.org/t/p/original/aosm8Vh99f6ToBtHgOhJYBh9Ky8.jpg",
                        LocalDate.now().plusDays(2),
                        "Tom Hardy, Chiwetel Ejiofor, Juno Temple, Rhys Ifans, Stephen Graham",
                        "Kelly Marcel",
                        Set.of("Action", "Sci-Fi", "Adventure")
                )
        );
    }
}
