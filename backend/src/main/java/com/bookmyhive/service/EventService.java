package com.bookmyhive.service;

import com.bookmyhive.dto.EventDto;
import com.bookmyhive.entity.Event;
import com.bookmyhive.entity.EventType;
import com.bookmyhive.entity.Genre;
import com.bookmyhive.exception.ConflictException;
import com.bookmyhive.exception.ResourceNotFoundException;
import com.bookmyhive.mapper.EntityMapper;
import com.bookmyhive.repository.BookingRepository;
import com.bookmyhive.repository.EventRepository;
import com.bookmyhive.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final GenreRepository genreRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<EventDto.Response> search(EventType type, String q, String genre, String language) {
        return eventRepository.search(type, clean(q), clean(genre), clean(language))
                .stream().map(EntityMapper::toEvent).toList();
    }

    @Transactional(readOnly = true)
    public List<EventDto.Response> findAllForAdmin() {
        return eventRepository.findAllByOrderByIdDesc().stream().map(EntityMapper::toEvent).toList();
    }

    @Transactional(readOnly = true)
    public EventDto.Response findById(Long id) {
        return EntityMapper.toEvent(find(id));
    }

    @Transactional
    public EventDto.Response create(EventDto.Request req) {
        Event event = new Event();
        apply(event, req);
        return EntityMapper.toEvent(eventRepository.save(event));
    }

    @Transactional
    public EventDto.Response update(Long id, EventDto.Request req) {
        Event event = find(id);
        apply(event, req);
        return EntityMapper.toEvent(event);
    }

    @Transactional
    public void delete(Long id) {
        Event event = find(id);
        if (bookingRepository.existsByShowEventId(id)) {
            throw new ConflictException("This event already has bookings. Mark it inactive instead of deleting it");
        }
        eventRepository.delete(event); // cascades to its (booking-free) shows
    }

    private void apply(Event event, EventDto.Request req) {
        event.setTitle(req.title().trim());
        event.setType(req.type());
        event.setDescription(req.description().trim());
        event.setLanguage(req.language().trim());
        event.setDurationMinutes(req.durationMinutes());
        event.setCertification(blankToNull(req.certification()));
        event.setRating(req.rating());
        event.setPosterUrl(blankToNull(req.posterUrl()));
        event.setReleaseDate(req.releaseDate());
        event.setActive(req.active() == null || req.active());
        event.setTmdbId(req.tmdbId());
        event.setBackdropUrl(blankToNull(req.backdropUrl()));
        event.setCast(blankToNull(req.cast()));
        event.setDirector(blankToNull(req.director()));

        Set<Long> ids = req.genreIds() == null ? Set.of() : req.genreIds();
        List<Genre> found = genreRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more genres do not exist");
        }
        event.setGenres(new HashSet<>(found));
    }

    private Event find(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
