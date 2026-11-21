package uoa.se325.lab03.model.library;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class User {

    @JsonProperty("libraryCardNumber")
    private String libraryCardNumber;
    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;
    @JsonProperty("dateJoined")
    private LocalDate dateJoined;
    @JsonProperty("name")
    private String name;
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "barCode")
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("borrowedMedia")
    private List<Media> borrowedMedia = new ArrayList<>();

    public User() {
    }

    public User(String libraryCardNumber, LocalDate dateOfBirth, LocalDate dateJoined, String name) {
        this.libraryCardNumber = libraryCardNumber;
        this.dateOfBirth = dateOfBirth;
        this.dateJoined = dateJoined;
        this.name = name;
    }

    public String getLibraryCardNumber() {
        return libraryCardNumber;
    }

    public void setLibraryCardNumber(String libraryCardNumber) {
        this.libraryCardNumber = libraryCardNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public LocalDate getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(LocalDate dateJoined) {
        this.dateJoined = dateJoined;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Media> getBorrowedMedia() {
        return Collections.unmodifiableList(this.borrowedMedia);
    }

    protected void addMedia(Media media) {
        this.borrowedMedia.add(media);
    }

    protected void removeMedia(Media media) {
        this.borrowedMedia.remove(media);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return libraryCardNumber.equals(user.libraryCardNumber) && dateOfBirth.equals(user.dateOfBirth) && dateJoined.equals(user.dateJoined) && name.equals(user.name) && borrowedMedia.equals(user.borrowedMedia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(libraryCardNumber, dateOfBirth, dateJoined, name, borrowedMedia);
    }
}
