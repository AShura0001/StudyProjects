package uoa.se325.assignment01.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uoa.se325.assignment01.model.Flight;
import uoa.se325.assignment01.repository.AirportRepository;
import uoa.se325.assignment01.repository.FlightRepository;
import uoa.se325.assignment01.utils.TimeZoneUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;

    public FlightController(FlightRepository flightRepository, AirportRepository airportRepository) {
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
    }

    @GetMapping
    public ResponseEntity<List<Flight>> search(@RequestParam String origin,
                                                @RequestParam String destination,
                                                @RequestParam(required = false) LocalDate departureDate,
                                                @RequestParam(defaultValue = "0") int dayRange) {
        if (dayRange < 0) {
            return ResponseEntity.badRequest().build();
        }

        LocalDateTime minDepartureTime = null;
        LocalDateTime maxDepartureTime = null;

        if (departureDate != null) {
            // A search term can match more than one airport (different time zones) - since the
            // spec doesn't cover that case, we just use the first match's time zone.
            List<String> timeZones = airportRepository.findDistinctTimeZonesMatching(origin);
            String timeZone = timeZones.isEmpty() ? "UTC" : timeZones.get(0);

            ZonedDateTime[] range = TimeZoneUtils.toZonedDateRange(departureDate, dayRange, timeZone);
            minDepartureTime = range[0].toLocalDateTime();
            maxDepartureTime = range[1].toLocalDateTime();
        }

        return ResponseEntity.ok(flightRepository.search(origin, destination, minDepartureTime, maxDepartureTime));
    }
}
