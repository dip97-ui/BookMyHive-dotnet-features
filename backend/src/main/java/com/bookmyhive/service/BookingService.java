package com.bookmyhive.service;

import com.bookmyhive.dto.BookingDto;
import com.bookmyhive.entity.*;
import com.bookmyhive.exception.BadRequestException;
import com.bookmyhive.exception.ConflictException;
import com.bookmyhive.exception.ResourceNotFoundException;
import com.bookmyhive.mapper.EntityMapper;
import com.bookmyhive.repository.BookingRepository;
import com.bookmyhive.repository.ShowRepository;
import com.bookmyhive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Pattern SEAT_PATTERN = Pattern.compile("^([A-Z])(\\d{1,2})$");

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;

    /**
     * Books seats. The show row is locked for the duration of the transaction so that two
     * concurrent requests cannot both reserve the same seat.
     */
    @Transactional
    public BookingDto.Response create(String email, BookingDto.Request req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Show show = showRepository.findByIdForUpdate(req.showId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id " + req.showId()));

        if (!show.getShowTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("This show has already started");
        }

        Set<String> seats = req.seats().stream()
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        seats.forEach(seat -> validateSeat(show, seat));

        Set<String> taken = new HashSet<>(bookingRepository.findBookedSeatLabels(show.getId()));
        List<String> clashes = seats.stream().filter(taken::contains).toList();
        if (!clashes.isEmpty()) {
            throw new ConflictException("Seats already booked: " + String.join(", ", clashes)
                    + ". Please pick different seats");
        }

        BigDecimal total = show.getPrice().multiply(BigDecimal.valueOf(seats.size()));

        Booking booking = new Booking();
        booking.setReference("BMH" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeats(seats);
        booking.setTotalAmount(total);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(LocalDateTime.now());

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod(req.paymentMethod() == null || req.paymentMethod().isBlank() ? "UPI" : req.paymentMethod());
        payment.setAmount(total);
        payment.setStatus("SUCCESS");
        payment.setTransactionId("TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        booking.setPayment(payment);

        return EntityMapper.toBooking(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingDto.Response> findMine(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bookingRepository.findByUserIdOrderByBookedAtDesc(user.getId())
                .stream().map(EntityMapper::toBooking).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto.Response> findAll() {
        return bookingRepository.findAllByOrderByBookedAtDesc().stream().map(EntityMapper::toBooking).toList();
    }

    /** Owner or admin may cancel a confirmed booking until the show starts. Seats become free again. */
    @Transactional
    public BookingDto.Response cancel(Long id, String email, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
        if (!isAdmin && !booking.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("Not your booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("This booking is already cancelled");
        }
        if (!booking.getShow().getShowTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Bookings cannot be cancelled after the show has started");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus("REFUNDED");
        }
        return EntityMapper.toBooking(booking);
    }

    private void validateSeat(Show show, String seat) {
        Matcher m = SEAT_PATTERN.matcher(seat);
        if (!m.matches()) {
            throw new BadRequestException("Invalid seat label: " + seat);
        }
        int row = m.group(1).charAt(0) - 'A';
        int number = Integer.parseInt(m.group(2));
        if (row >= show.getSeatRows() || number < 1 || number > show.getSeatsPerRow()) {
            throw new BadRequestException("Seat " + seat + " does not exist in this show");
        }
    }
}
