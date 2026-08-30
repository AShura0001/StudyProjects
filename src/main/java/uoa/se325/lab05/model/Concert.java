package uoa.se325.lab05.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a Concert that is taking place. Concerts may take place on multiple dates / times. Concerts may have any
 * number of Performers. Performers may perform in any number of Concerts.
 */
@Entity
public class Concert implements Comparable<Concert> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime startTime;

    private int durationInMinutes;

    private int standardTicketPrice;

    private Integer specialTicketPrice;

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private List<Performer> performers = new ArrayList<>();

    public Concert() {
    }

    public Concert(String title, LocalDateTime startTime) {
        this(title, startTime, 240, 150);
    }

    public Concert(String title, LocalDateTime startTime, int durationInMinutes, int standardTicketPrice) {
        this.title = title;
        this.startTime = startTime;
        this.durationInMinutes = durationInMinutes;
        this.standardTicketPrice = standardTicketPrice;
    }

    public Concert(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public List<Performer> getPerformers() {
        return performers;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(int duration) {
        this.durationInMinutes = duration;
    }

    public int getStandardTicketPrice() {
        return standardTicketPrice;
    }

    public void setStandardTicketPrice(int standardTicketPrice) {
        this.standardTicketPrice = standardTicketPrice;
    }

    public Integer getSpecialTicketPrice() {
        return specialTicketPrice;
    }

    public void setSpecialTicketPrice(Integer specialTicketPrice) {
        this.specialTicketPrice = specialTicketPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Concert concert = (Concert) o;
        return durationInMinutes == concert.durationInMinutes && standardTicketPrice == concert.standardTicketPrice && Objects.equals(id, concert.id) && Objects.equals(title, concert.title) && Objects.equals(startTime, concert.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, startTime, durationInMinutes, standardTicketPrice);
    }

    @Override
    public int compareTo(Concert concert) {
        return title.compareTo(concert.getTitle());
    }
}
