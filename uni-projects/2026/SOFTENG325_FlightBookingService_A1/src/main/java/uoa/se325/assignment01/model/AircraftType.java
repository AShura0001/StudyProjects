package uoa.se325.assignment01.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // Not exposed in JSON - the API only ever shows an aircraft type's id/name. Zones are used
    // server-side to work out a seat's cabin class.
    @ElementCollection
    @JsonIgnore
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

    // Splits a seat code like "12A" into row 12 + letter "A", and finds which zone (if any) it
    // falls in. Empty if the code is malformed or doesn't belong to any zone on this aircraft.
    public Optional<CabinClass> getCabinClassForSeat(String seatCode) {
        if (seatCode == null || seatCode.length() < 2) {
            return Optional.empty();
        }

        String letter = seatCode.substring(seatCode.length() - 1);
        int row;
        try {
            row = Integer.parseInt(seatCode.substring(0, seatCode.length() - 1));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        return seatingZones.stream()
                .filter(zone -> row >= zone.getStartingRowNumber() && row < zone.getStartingRowNumber() + zone.getNumRows())
                .filter(zone -> zone.getSeatCodeData().contains(letter))
                .map(SeatingZone::getCabinClass)
                .findFirst();
    }
}
