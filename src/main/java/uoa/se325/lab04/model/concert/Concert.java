package uoa.se325.lab04.model.concert;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;

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

    @ElementCollection
    @CollectionTable(name = "concert_date", joinColumns = @JoinColumn(name = "concert_id"))
    @OrderColumn(name = "date_index")
    private List<LocalDateTime> dates = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "concert_performer",
            joinColumns = @JoinColumn(name = "concert_id"),
            inverseJoinColumns = @JoinColumn(name = "performer_id")
    )
    @OrderColumn(name = "performer_index")
    private List<Performer> performers = new ArrayList<>();

    public Concert() {
    }

    public Concert(String title, LocalDateTime... dates) {
        this.title = title;
        this.dates.addAll(List.of(dates));
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

    public List<LocalDateTime> getDates() {
        return dates;
    }

    public List<Performer> getPerformers() {
        return performers;
    }

    public boolean isScheduledOn(LocalDateTime date) {
        return dates.contains(date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Concert concert = (Concert) o;
        return Objects.equals(title, concert.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

    @Override
    public int compareTo(Concert concert) {
        return title.compareTo(concert.getTitle());
    }
}
