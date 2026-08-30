package uoa.se325.lab05.controller;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uoa.se325.lab05.dto.NewConcertRequest;
import uoa.se325.lab05.model.Concert;
import uoa.se325.lab05.repository.ConcertRepository;
import uoa.se325.lab05.repository.PerformerRepository;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertRepository concertRepository;
    private final PerformerRepository performerRepository;

    @Autowired
    public ConcertController(ConcertRepository concertRepository, PerformerRepository performerRepository) {
        this.concertRepository = concertRepository;
        this.performerRepository = performerRepository;
    }

    /**
     * Gets the Concert with the given id, if any, including information about that Concert's Performers.
     *
     * @param id the id to query, as a path parameter
     * @return a 200 OK response with the given Concert entity if successful, or a 404 Not Found response if a Concert
     * with the given id doesn't exist.
     */
    @GetMapping("/{id}")
    public Concert getConcert(@PathVariable("id") long id) {
        return concertRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concert not found"));
    }

    /**
     * Gets all concerts.
     *
     * @return a 200 OK response, with all concerts in the system in the body.
     */
    @GetMapping
    public List<Concert> getAllConcerts() {
        return concertRepository.findAll();
    }

    /**
     * Creates a new Concert in the database, with the information in the supplied
     * {@link NewConcertRequest} instance from the HTTP request body.
     *
     * @param request the info about the new concert to create
     * @return a 201 Created response, with the URI of the newly created Concert, or a 409 Conflict response if the
     * new concert's date/time range overlaps with an existing concert.
     */
    @PostMapping
    @org.springframework.transaction.annotation.Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<Void> createConcert(@RequestBody NewConcertRequest request) {
        var concert = request.toConcert();

        LocalDateTime endTime = concert.getStartTime().plusMinutes(concert.getDurationInMinutes());
        var overlapping = concertRepository.findOverlappingConcerts(concert.getStartTime(), endTime);
        if (!overlapping.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Concert overlaps with an existing concert");
        }

        concertRepository.save(concert);
        return ResponseEntity.created(URI.create("/concerts/" + concert.getId())).build();
    }

    /**
     * Modifies the Concert with the given id, by setting its list of Performers to those Performers with the given ids.
     * If the given list of ids contains any values which don't correspond to valid Performer ids, then those values
     * will be ignored. The given list completely replaces any existing Performers for the matching Concert, rather
     * than adding to it.
     *
     * @param concertId    the id of the Concert to modify
     * @param performerIds the list of performer ids. This list will completely replace any performers which this
     *                     Concert already had, rather than adding to it.
     * @return a 204 No Content response if successful, or a 404 Not Found response if the Concert with the given id
     * doesn't exist.
     */
    @PutMapping("/{id}/performers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void updateConcertPerformers(@PathVariable("id") long concertId, @RequestBody List<Long> performerIds) {
        var concert = getConcert(concertId);
        var performers = performerRepository.findAllById(performerIds);

        concert.getPerformers().clear();
        concert.getPerformers().addAll(performers);
        performers.forEach(p -> p.getConcerts().add(concert));

        // Should only need to save the concert - state should be cascaded to performers
        // if annotations are set up correctly.
        concertRepository.save(concert);
    }
}
