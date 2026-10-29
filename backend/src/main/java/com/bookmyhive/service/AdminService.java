package com.bookmyhive.service;

import com.bookmyhive.dto.StatsDto;
import com.bookmyhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StatsDto stats() {
        return new StatsDto(eventRepository.count(), venueRepository.count(), showRepository.count(),
                bookingRepository.count(), userRepository.count(), bookingRepository.totalRevenue());
    }
}
