package com.bookmyhive.service;

import com.bookmyhive.dto.VenueDto;
import com.bookmyhive.entity.Venue;
import com.bookmyhive.exception.ConflictException;
import com.bookmyhive.exception.ResourceNotFoundException;
import com.bookmyhive.mapper.EntityMapper;
import com.bookmyhive.repository.ShowRepository;
import com.bookmyhive.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public List<VenueDto.Response> findAll() {
        return venueRepository.findAllByOrderByCityAscNameAsc().stream().map(EntityMapper::toVenue).toList();
    }

    @Transactional(readOnly = true)
    public VenueDto.Response findById(Long id) {
        return EntityMapper.toVenue(find(id));
    }

    @Transactional
    public VenueDto.Response create(VenueDto.Request req) {
        return EntityMapper.toVenue(venueRepository.save(
                new Venue(req.name().trim(), req.city().trim(), req.address().trim())));
    }

    @Transactional
    public VenueDto.Response update(Long id, VenueDto.Request req) {
        Venue venue = find(id);
        venue.setName(req.name().trim());
        venue.setCity(req.city().trim());
        venue.setAddress(req.address().trim());
        return EntityMapper.toVenue(venue);
    }

    @Transactional
    public void delete(Long id) {
        Venue venue = find(id);
        if (showRepository.existsByVenueId(id)) {
            throw new ConflictException("Venue still has shows. Delete those shows first");
        }
        venueRepository.delete(venue);
    }

    private Venue find(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id " + id));
    }
}
