package uoa.se325.lab05.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uoa.se325.lab05.model.Performer;
import uoa.se325.lab05.repository.PerformerRepository;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/performers")
public class PerformerController {

    private final PerformerRepository performerRepository;

    @Autowired
    public PerformerController(PerformerRepository performerRepository) {
        this.performerRepository = performerRepository;
    }

    /**
     * Gets the Performer with the given id, if any.
     *
     * @param id the id to query, as a path parameter
     * @return a 200 OK response with the given Performer entity if successful, or a 404 Not Found response if a
     * Performer with the given id doesn't exist.
     */
    @GetMapping("/{id}")
    public Performer getPerformer(@PathVariable("id") long id) {
        return performerRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Performer not found"));
    }

    /**
     * Gets all Performers.
     *
     * @return a 200 OK response, with all Performers in the system in the body.
     */
    @GetMapping
    public List<Performer> getAllPerformers() {
        return performerRepository.findAll();
    }

    /**
     * Creates a new Performer in the database, with the information in the supplied {@link Performer} instance
     * from the HTTP request body. If the Performer corresponds to a {@link Band}, then any of that Band's members
     * will also be added as individual Performers.
     *
     * @param performer the Performer to create
     * @return a 201 Created response, with the URI of the newly created Performer.
     */
    @PostMapping
    public ResponseEntity<Void> createPerformer(@RequestBody Performer performer) {
        performerRepository.save(performer);
        return ResponseEntity.created(URI.create("/performers/" + performer.getId())).build();
    }
}
