package uoa.se325.assignment01.dto;

import uoa.se325.assignment01.model.Booking;
import uoa.se325.assignment01.model.Flight;

import java.util.Comparator;
import java.util.List;

// Booking's own fields (minus the user) plus a computed totalCost - avoids duplicating
// Booking/Flight's fields in a separate response shape.
public record BookingDTO(Long id, List<String> bookedSeats, int totalCost, Flight flight) {

    public static BookingDTO from(Booking booking) {
        Flight flight = booking.getFlight();

        List<String> sortedSeats = booking.getBookedSeats().stream()
                .sorted(Comparator.comparingInt(BookingDTO::rowOf).thenComparing(BookingDTO::letterOf))
                .toList();

        int totalCost = booking.getBookedSeats().stream()
                .mapToInt(seat -> flight.getAircraftType().getCabinClassForSeat(seat)
                        .map(cabinClass -> flight.getSeatPricings().get(cabinClass))
                        .orElse(0))
                .sum();

        return new BookingDTO(booking.getId(), sortedSeats, totalCost, flight);
    }

    private static int rowOf(String seatCode) {
        return Integer.parseInt(seatCode.substring(0, seatCode.length() - 1));
    }

    private static String letterOf(String seatCode) {
        return seatCode.substring(seatCode.length() - 1);
    }
}
