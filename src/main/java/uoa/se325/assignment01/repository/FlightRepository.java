package uoa.se325.assignment01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uoa.se325.assignment01.model.Flight;

import java.time.LocalDateTime;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    /**
     * Searches for flights whose origin and destination airport names/codes contain the given
     * search terms (case-insensitive), optionally restricted to departing within a given local
     * date/time range. Pass {@code null} for {@code minDepartureTime} and {@code maxDepartureTime}
     * to skip the date range restriction. Results are ordered by departure time, ascending.
     *
     * @param origin            search term to match against the origin airport's name/code
     * @param destination       search term to match against the destination airport's name/code
     * @param minDepartureTime  the earliest local departure time to match (inclusive), or null for no lower bound
     * @param maxDepartureTime  the latest local departure time to match (inclusive), or null for no upper bound
     */
    @Query("""
            SELECT f FROM Flight f
            WHERE (LOWER(f.origin.name) LIKE LOWER(CONCAT('%', :origin, '%')) OR LOWER(f.origin.code) LIKE LOWER(CONCAT('%', :origin, '%')))
            AND (LOWER(f.destination.name) LIKE LOWER(CONCAT('%', :destination, '%')) OR LOWER(f.destination.code) LIKE LOWER(CONCAT('%', :destination, '%')))
            AND (:minDepartureTime IS NULL OR f.departureTimeLocal BETWEEN :minDepartureTime AND :maxDepartureTime)
            ORDER BY f.departureTimeLocal ASC
            """)
    List<Flight> search(@Param("origin") String origin,
                         @Param("destination") String destination,
                         @Param("minDepartureTime") LocalDateTime minDepartureTime,
                         @Param("maxDepartureTime") LocalDateTime maxDepartureTime);
}
