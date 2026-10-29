package com.bookmyhive.service;

import com.bookmyhive.dto.GenreDto;
import com.bookmyhive.entity.Genre;
import com.bookmyhive.exception.ConflictException;
import com.bookmyhive.exception.ResourceNotFoundException;
import com.bookmyhive.mapper.EntityMapper;
import com.bookmyhive.repository.EventRepository;
import com.bookmyhive.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<GenreDto.Response> findAll() {
        return genreRepository.findAllByOrderByNameAsc().stream().map(EntityMapper::toGenre).toList();
    }

    @Transactional
    public GenreDto.Response create(GenreDto.Request req) {
        String name = req.name().trim();
        if (genreRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Genre '" + name + "' already exists");
        }
        return EntityMapper.toGenre(genreRepository.save(new Genre(name)));
    }

    @Transactional
    public GenreDto.Response update(Long id, GenreDto.Request req) {
        Genre genre = find(id);
        String name = req.name().trim();
        if (!genre.getName().equalsIgnoreCase(name) && genreRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Genre '" + name + "' already exists");
        }
        genre.setName(name);
        return EntityMapper.toGenre(genre);
    }

    @Transactional
    public void delete(Long id) {
        Genre genre = find(id);
        if (eventRepository.existsByGenresId(id)) {
            throw new ConflictException("Genre is used by events. Remove it from them first");
        }
        genreRepository.delete(genre);
    }

    private Genre find(Long id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id " + id));
    }
}
