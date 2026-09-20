package uoa.se325.assignment01.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a flight between an {@link #origin} and {@link #destination} airport, on a
 * particular {@link AircraftType}.
 * <p>
 * Departure and arrival times are persisted as the "local" (zone-naive) wall-clock time at the
 * relevant airport, since that's how they're recorded in the database. The public
 * {@link #getDepartureTime()} / {@link #getArrivalTime()} accessors combine these with the
 * origin/destination airports' time zones to expose proper {@link ZonedDateTime} values, as
 * required by the domain model.
 */
@Entity
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    private AircraftType aircraftType;

    @ManyToOne
    private Airport origin;

    @ManyToOne
    private Airport destination;

    @Column(name = "departure_time")
    private LocalDateTime departureTimeLocal;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTimeLocal;

    @Enumerated(EnumType.STRING)
    private FlightStatus flightStatus;

    // Not exposed directly in JSON - booking-info repackages this under "pricingInfo" instead.
    @ElementCollection
    @MapKeyColumn(name = "cabin_class")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "price")
    @JsonIgnore
    private Map<CabinClass, Integer> seatPricings = new HashMap<>();

    /**
     * Bookings already made for this flight. Not exposed in JSON - it's here to complete the
     * domain model as described in the assignment spec, but lookups of a flight's booked seats
     * go through {@code BookingRepository} directly rather than navigating this collection, so
     * it's left lazily-fetched.
     */
    @OneToMany(mappedBy = "flight")
    @JsonIgnore
    private List<Booking> bookings = new ArrayList<>();

    public Flight() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AircraftType getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(AircraftType aircraftType) {
        this.aircraftType = aircraftType;
    }

    public Airport getOrigin() {
        return origin;
    }

    public void setOrigin(Airport origin) {
        this.origin = origin;
    }

    public Airport getDestination() {
        return destination;
    }

    public void setDestination(Airport destination) {
        this.destination = destination;
    }

    @JsonIgnore
    public LocalDateTime getDepartureTimeLocal() {
        return departureTimeLocal;
    }

    public void setDepartureTimeLocal(LocalDateTime departureTimeLocal) {
        this.departureTimeLocal = departureTimeLocal;
    }

    @JsonIgnore
    public LocalDateTime getArrivalTimeLocal() {
        return arrivalTimeLocal;
    }

    public void setArrivalTimeLocal(LocalDateTime arrivalTimeLocal) {
        this.arrivalTimeLocal = arrivalTimeLocal;
    }

    /**
     * @return the flight's departure time, as a {@link ZonedDateTime} in the origin airport's time zone.
     */
    public ZonedDateTime getDepartureTime() {
        return ZonedDateTime.of(departureTimeLocal, ZoneId.of(origin.getTimeZone()));
    }

    /**
     * @return the flight's arrival time, as a {@link ZonedDateTime} in the destination airport's time zone.
     */
    public ZonedDateTime getArrivalTime() {
        return ZonedDateTime.of(arrivalTimeLocal, ZoneId.of(destination.getTimeZone()));
    }

    public FlightStatus getFlightStatus() {
        return flightStatus;
    }

    public void setFlightStatus(FlightStatus flightStatus) {
        this.flightStatus = flightStatus;
    }

    public Map<CabinClass, Integer> getSeatPricings() {
        return seatPricings;
    }

    public void setSeatPricings(Map<CabinClass, Integer> seatPricings) {
        this.seatPricings = seatPricings;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
}
