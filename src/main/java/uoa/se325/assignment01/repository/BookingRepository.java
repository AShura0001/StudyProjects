package uoa.se325.assignment01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uoa.se325.assignment01.model.Booking;
import uoa.se325.assignment01.model.Flight;
import uoa.se325.assignment01.model.User;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserOrderByFlight_DepartureTimeLocalAsc(User user);

    Optional<Booking> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    /**
     * @return true if any of the given seat codes are already booked on the given flight.
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b JOIN b.bookedSeats s WHERE b.flight = :flight AND s IN :seats")
    boolean existsConflictingBooking(@Param("flight") Flight flight, @Param("seats") List<String> seats);
}
