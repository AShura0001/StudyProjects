package uoa.se325.assignment01.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one user's booking of one or more seats on a particular flight.
 * <p>
 * The {@link #user} who made the booking is not exposed in JSON - only the {@link #flight} and
 * {@link #bookedSeats} are relevant to a client asking about their own bookings, since the user
 * is already implied by the authenticated request.
 */
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private User user;

    @ManyToOne
    private Flight flight;

    @ElementCollection
    private List<String> bookedSeats = new ArrayList<>();

    public Booking() {
    }

    public Booking(User user, Flight flight, List<String> bookedSeats) {
        this.user = user;
        this.flight = flight;
        this.bookedSeats = bookedSeats;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    public List<String> getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(List<String> bookedSeats) {
        this.bookedSeats = bookedSeats;
    }
}
