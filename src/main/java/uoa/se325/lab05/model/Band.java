package uoa.se325.lab05.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Band. Bands can contain multiple Members, each of which is a Performer in their own right. Performers
 * can be members of multiple Bands.
 */
@Entity
public class Band extends Performer {

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<IndividualArtist> members = new ArrayList<>();

    public Band() {
    }

    public Band(Long id, String name, Genre genre) {
        super(id, name, genre);
    }

    public Band(String name, Genre genre, IndividualArtist... members) {
        super(name, genre);
        this.members.addAll(List.of(members));
    }

    public List<IndividualArtist> getMembers() {
        return members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Band band = (Band) o;
        return Objects.equals(members, band.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), members);
    }
}
