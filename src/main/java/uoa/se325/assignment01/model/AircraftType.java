package uoa.se325.assignment01.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a kind of aircraft on which a {@link Flight} can be taken, including its
 * available {@link SeatingZone}s.
 */
@Entity
public class AircraftType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ElementCollection
    private List<SeatingZone> seatingZones = new ArrayList<>();

    public AircraftType() {
    }

    public AircraftType(String name) {
        this.name = name;
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

    public List<SeatingZone> getSeatingZones() {
        return seatingZones;
    }

    public void setSeatingZones(List<SeatingZone> seatingZones) {
        this.seatingZones = seatingZones;
    }
}
