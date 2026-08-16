package uoa.se325.lab03.model.library;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Book.class, name = "book"),
        @JsonSubTypes.Type(value = Film.class, name = "film")
})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "barCode")
public abstract class Media {

    private String barCode;
    private String title;
    private Genre[] genres;
    @JsonSerialize(using = BufferedImageJsonConverter.Serializer.class)
    @JsonDeserialize(using = BufferedImageJsonConverter.Deserializer.class)
    private BufferedImage coverImage;

    public Media() {

    }

    public Media(String barCode, String title, Genre... genres) {
        this.barCode = barCode;
        this.title = title;
        this.genres = genres;
    }

    public BufferedImage getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(BufferedImage coverImage) {
        this.coverImage = coverImage;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Genre[] getGenres() {
        return genres;
    }

    public void setGenres(Genre[] genres) {
        this.genres = genres;
    }

    @JsonProperty("mediaType")
    public String getMediaType() {
        return this.getClass().getSimpleName();
    }

    @JsonProperty("mediaType")
    public void setMediaType(String mediaType) {
        // Ignored - mediaType is derived from class type
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Media media = (Media) o;
        return barCode.equals(media.barCode) && title.equals(media.title) && Arrays.equals(genres, media.genres);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(barCode, title);
        result = 31 * result + Arrays.hashCode(genres);
        return result;
    }
}
