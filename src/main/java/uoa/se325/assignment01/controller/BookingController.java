package uoa.se325.assignment01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uoa.se325.assignment01.auth.AuthenticatedUser;
import uoa.se325.assignment01.dto.BookingRequest;
import uoa.se325.assignment01.model.Booking;
import uoa.se325.assignment01.model.Flight;
import uoa.se325.assignment01.model.User;
import uoa.se325.assignment01.repository.BookingRepository;
import uoa.se325.assignment01.repository.FlightRepository;

import java.net.URI;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    public BookingController(BookingRepository bookingRepository, FlightRepository flightRepository) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> createBooking(@RequestBody BookingRequest request, @AuthenticatedUser User user) {
        Flight flight = flightRepository.findById(request.flightId()).orElse(null);
        if (flight == null) {
            return ResponseEntity.notFound().build();
        }

        boolean allSeatsValid = request.requestedSeats().stream()
                .allMatch(seat -> flight.getAircraftType().getCabinClassForSeat(seat).isPresent());
        if (!allSeatsValid) {
            return ResponseEntity.notFound().build();
        }

        if (bookingRepository.existsConflictingBooking(flight, request.requestedSeats())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Booking booking = bookingRepository.save(new Booking(user, flight, request.requestedSeats()));

        return ResponseEntity.created(URI.create("/api/bookings/" + booking.getId())).build();
    }
}
