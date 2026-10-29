package com.bookmyhive.repository;

import com.bookmyhive.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Genre> findByNameIgnoreCase(String name);

    List<Genre> findAllByOrderByNameAsc();
}
