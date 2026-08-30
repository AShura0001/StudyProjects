package uoa.se325.lab05.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uoa.se325.lab05.model.Concert;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {

    /**
     * Finds any existing concerts whose [startTime, endTime) window overlaps with the given window.
     */
    @Query("SELECT c FROM Concert c WHERE c.startTime < :endTime " +
            "AND function('timestampadd', MINUTE, c.durationInMinutes, c.startTime) > :startTime")
    List<Concert> findOverlappingConcerts(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
}
