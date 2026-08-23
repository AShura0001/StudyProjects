package uoa.se325.lab04.model.concert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.*;

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
public abstract class Performer {

    private Long id;

    private String name;

    private Genre genre;

    @JsonIgnore
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
