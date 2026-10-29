package com.bookmyhive.repository;

import com.bookmyhive.entity.Event;
import com.bookmyhive.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** Public catalogue search. Text filters use "" to mean "no filter"; type uses null. */
    @Query("""
            select distinct e from Event e left join e.genres g
            where e.active = true
              and (:type is null or e.type = :type)
              and (:q = '' or lower(e.title) like lower(concat('%', :q, '%')))
              and (:genre = '' or lower(g.name) = lower(:genre))
              and (:language = '' or lower(e.language) = lower(:language))
            order by e.releaseDate desc
            """)
    List<Event> search(@Param("type") EventType type,
                       @Param("q") String q,
                       @Param("genre") String genre,
                       @Param("language") String language);

    List<Event> findAllByOrderByIdDesc();

    Optional<Event> findByTmdbId(Long tmdbId);

    Optional<Event> findByTitleIgnoreCase(String title);

    boolean existsByGenresId(Long genreId);
}
