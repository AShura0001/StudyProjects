package uoa.se325.assignment01.dto;

import java.util.List;

public record BookingRequest(Long flightId, List<String> requestedSeats) {
}
