package uoa.se325.assignment01.dto;

import uoa.se325.assignment01.model.AircraftType;
import uoa.se325.assignment01.model.CabinClass;

import java.util.List;
import java.util.Map;

// Combines info from Flight and AircraftType into the shape the booking page needs -
// aircraft type, already-booked seats, and per-cabin pricing.
public record BookingInfoDTO(AircraftType aircraftType, List<String> bookedSeats, Map<CabinClass, Integer> pricingInfo) {
}
