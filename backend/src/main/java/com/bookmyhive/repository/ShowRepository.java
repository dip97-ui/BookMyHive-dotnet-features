package com.bookmyhive.repository;

import com.bookmyhive.entity.Show;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
            select s from Show s join fetch s.venue v join fetch s.event e
            where e.id = :eventId and s.showTime > :now
              and (:city = '' or lower(v.city) = lower(:city))
            order by s.showTime
            """)
    List<Show> findUpcomingByEvent(@Param("eventId") Long eventId,
                                   @Param("now") LocalDateTime now,
                                   @Param("city") String city);

    @Query("select s from Show s join fetch s.venue join fetch s.event order by s.showTime desc")
    List<Show> findAllWithDetails();

    /** Row lock so two people cannot book the same seat at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Show s where s.id = :id")
    Optional<Show> findByIdForUpdate(@Param("id") Long id);

    boolean existsByVenueId(Long venueId);

    boolean existsByEventId(Long eventId);
}
