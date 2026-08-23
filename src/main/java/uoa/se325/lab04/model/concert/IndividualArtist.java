package uoa.se325.lab04.model.concert;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an individual artist - a solo / single person capable of performing at concerts. Individual artists can
 * perform by themselves or as members of Bands.
 */
public class IndividualArtist extends Performer {

    private LocalDate dateOfBirth;

    public IndividualArtist() {
        super();
    }

    public IndividualArtist(Long id, String name, Genre genre, LocalDate dateOfBirth) {
        super(id, name, genre);
        this.dateOfBirth = dateOfBirth;
    }

    public IndividualArtist(String name, Genre genre, LocalDate dateOfBirth) {
        super(name, genre);
        this.dateOfBirth = dateOfBirth;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IndividualArtist that = (IndividualArtist) o;
        return Objects.equals(dateOfBirth, that.dateOfBirth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dateOfBirth);
    }
}
