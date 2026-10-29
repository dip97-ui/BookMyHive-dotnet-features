package com.bookmyhive.service;

import com.bookmyhive.dto.ShowDto;
import com.bookmyhive.entity.Event;
import com.bookmyhive.entity.Show;
import com.bookmyhive.entity.Venue;
import com.bookmyhive.exception.ConflictException;
import com.bookmyhive.exception.ResourceNotFoundException;
import com.bookmyhive.mapper.EntityMapper;
import com.bookmyhive.repository.BookingRepository;
import com.bookmyhive.repository.EventRepository;
import com.bookmyhive.repository.ShowRepository;
import com.bookmyhive.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public ShowDto.Response findById(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<ShowDto.Response> findUpcomingByEvent(Long eventId, String city) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id " + eventId);
        }
        String cityFilter = city == null ? "" : city.trim();
        return showRepository.findUpcomingByEvent(eventId, LocalDateTime.now(), cityFilter)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ShowDto.Response> findAllForAdmin() {
        return showRepository.findAllWithDetails().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShowDto.Response create(ShowDto.Request req) {
        Show show = new Show();
        apply(show, req);
        return toResponse(showRepository.save(show));
    }

    @Transactional
    public ShowDto.Response update(Long id, ShowDto.Request req) {
        Show show = find(id);
        boolean layoutChanged = show.getSeatRows() != req.seatRows() || show.getSeatsPerRow() != req.seatsPerRow();
        if (layoutChanged && bookingRepository.existsByShowId(id)) {
            throw new ConflictException("Seat layout cannot change once the show has bookings");
        }
        apply(show, req);
        return toResponse(show);
    }

    @Transactional
    public void delete(Long id) {
        Show show = find(id);
        if (bookingRepository.existsByShowId(id)) {
            throw new ConflictException("This show has bookings and cannot be deleted");
        }
        showRepository.delete(show);
    }

    private void apply(Show show, ShowDto.Request req) {
        Event event = eventRepository.findById(req.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + req.eventId()));
        Venue venue = venueRepository.findById(req.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id " + req.venueId()));
        show.setEvent(event);
        show.setVenue(venue);
        show.setShowTime(req.showTime());
        show.setPrice(req.price());
        show.setSeatRows(req.seatRows());
        show.setSeatsPerRow(req.seatsPerRow());
    }

    private Show find(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id " + id));
    }

    private ShowDto.Response toResponse(Show show) {
        return EntityMapper.toShow(show, bookingRepository.findBookedSeatLabels(show.getId()));
    }
}
