package com.bookmyhive.repository;

import com.bookmyhive.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"show", "show.event", "show.venue", "user"})
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"show", "show.event", "show.venue", "user"})
    List<Booking> findAllByOrderByBookedAtDesc();

    @Query("""
            select seat from Booking b join b.seats seat
            where b.show.id = :showId
              and b.status = com.bookmyhive.entity.BookingStatus.CONFIRMED
            """)
    List<String> findBookedSeatLabels(@Param("showId") Long showId);

    boolean existsByShowId(Long showId);

    boolean existsByShowEventId(Long eventId);

    @Query("""
            select coalesce(sum(b.totalAmount), 0) from Booking b
            where b.status = com.bookmyhive.entity.BookingStatus.CONFIRMED
            """)
    BigDecimal totalRevenue();
}
