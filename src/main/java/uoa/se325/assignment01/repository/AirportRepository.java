package uoa.se325.assignment01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uoa.se325.assignment01.model.Airport;

import java.util.List;

public interface AirportRepository extends JpaRepository<Airport, Long> {

    // Used to resolve which time zone to search flights in, when a departure date is given.
    @Query("SELECT DISTINCT a.timeZone FROM Airport a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<String> findDistinctTimeZonesMatching(@Param("term") String term);
}
