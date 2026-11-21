package uoa.se325.lab03.model.library;

import java.util.Objects;

public class Film extends Media {

    private int lengthInMinutes;
    private Rating rating;
    private String director;

    public Film() {

    }

    public Film(String barCode, String title, Rating rating, int lengthInMinutes, String director, Genre... genres) {
        super(barCode, title, genres);
        this.lengthInMinutes = lengthInMinutes;
        this.rating = rating;
        this.director = director;
    }

    public int getLengthInMinutes() {
        return lengthInMinutes;
    }

    public void setLengthInMinutes(int lengthInMinutes) {
        this.lengthInMinutes = lengthInMinutes;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Film film = (Film) o;
        return lengthInMinutes == film.lengthInMinutes && rating.equals(film.rating) && director.equals(film.director);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lengthInMinutes, rating, director);
    }
}
