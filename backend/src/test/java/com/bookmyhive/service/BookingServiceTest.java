package com.bookmyhive.service;

import com.bookmyhive.dto.BookingDto;
import com.bookmyhive.entity.*;
import com.bookmyhive.exception.BadRequestException;
import com.bookmyhive.exception.ConflictException;
import com.bookmyhive.repository.BookingRepository;
import com.bookmyhive.repository.ShowRepository;
import com.bookmyhive.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String EMAIL = "user@test.com";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ShowRepository showRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Show show;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail(EMAIL);

        Event event = new Event();
        event.setId(10L);
        event.setTitle("Test Movie");
        event.setType(EventType.MOVIE);

        Venue venue = new Venue("Test Hall", "Mumbai", "Somewhere");
        venue.setId(5L);

        show = new Show();
        show.setId(100L);
        show.setEvent(event);
        show.setVenue(venue);
        show.setShowTime(LocalDateTime.now().plusDays(1));
        show.setPrice(new BigDecimal("200.00"));
        show.setSeatRows(5);
        show.setSeatsPerRow(10);
    }

    @Test
    void create_calculatesTotalAndConfirmsBooking() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(showRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(show));
        when(bookingRepository.findBookedSeatLabels(100L)).thenReturn(List.of("A1"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto.Response res = bookingService.create(EMAIL, new BookingDto.Request(100L, Set.of("B1", "B2"), "CARD"));

        assertEquals(0, new BigDecimal("400").compareTo(res.totalAmount()));
        assertEquals(BookingStatus.CONFIRMED, res.status());
        assertEquals("SUCCESS", res.paymentStatus());
        assertEquals(List.of("B1", "B2"), res.seats());
        assertTrue(res.reference().startsWith("BMH"));
    }

    @Test
    void create_rejectsSeatsThatAreAlreadyBooked() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(showRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(show));
        when(bookingRepository.findBookedSeatLabels(100L)).thenReturn(List.of("A1", "A2"));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> bookingService.create(EMAIL, new BookingDto.Request(100L, Set.of("A2", "A3"), null)));

        assertTrue(ex.getMessage().contains("A2"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_rejectsSeatOutsideTheLayout() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(showRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(show));

        assertThrows(BadRequestException.class,
                () -> bookingService.create(EMAIL, new BookingDto.Request(100L, Set.of("Z9"), null)));
        assertThrows(BadRequestException.class,
                () -> bookingService.create(EMAIL, new BookingDto.Request(100L, Set.of("A11"), null)));
    }

    @Test
    void create_rejectsShowThatAlreadyStarted() {
        show.setShowTime(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(showRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(show));

        assertThrows(BadRequestException.class,
                () -> bookingService.create(EMAIL, new BookingDto.Request(100L, Set.of("A1"), null)));
    }

    @Test
    void cancel_byOwnerMarksBookingCancelledAndRefunded() {
        Booking booking = existingBooking();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        BookingDto.Response res = bookingService.cancel(7L, EMAIL, false);

        assertEquals(BookingStatus.CANCELLED, res.status());
        assertEquals("REFUNDED", res.paymentStatus());
    }

    @Test
    void cancel_byAnotherUserIsDenied_butAdminIsAllowed() {
        Booking booking = existingBooking();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        assertThrows(AccessDeniedException.class, () -> bookingService.cancel(7L, "other@test.com", false));
        assertEquals(BookingStatus.CANCELLED, bookingService.cancel(7L, "admin@test.com", true).status());
    }

    private Booking existingBooking() {
        Booking b = new Booking();
        b.setId(7L);
        b.setReference("BMHTEST01");
        b.setUser(user);
        b.setShow(show);
        b.setSeats(Set.of("C3"));
        b.setTotalAmount(new BigDecimal("200.00"));
        Payment p = new Payment();
        p.setBooking(b);
        p.setMethod("UPI");
        p.setStatus("SUCCESS");
        b.setPayment(p);
        return b;
    }
}
