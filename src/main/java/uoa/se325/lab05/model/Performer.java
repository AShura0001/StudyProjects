package uoa.se325.lab05.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Performer, who can perform at concerts. Concerts may have any number of Performers; Performers may
 * perform at any number of Concerts.
 * <p>
 * This abstract class contains information common to both Bands and IndividualArtists.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "performerType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Band.class, name = "Band"),
        @JsonSubTypes.Type(value = IndividualArtist.class, name = "IndividualArtist")
})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Performer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    @JsonIgnore
    @ManyToMany(mappedBy = "performers")
    private List<Concert> performingIn = new ArrayList<>();

    public Performer() {
    }

    public Performer(Long id, String name, Genre genre) {
        this.id = id;
        this.name = name;
        this.genre = genre;
    }

    public Performer(String name, Genre genre) {
        this.name = name;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Genre getGenre() {
        return genre;
    }

    @JsonIgnore
    public List<Concert> getConcerts() {
        return performingIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Performer performer = (Performer) o;
        return Objects.equals(name, performer.name) && genre == performer.genre;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, genre);
    }
}
