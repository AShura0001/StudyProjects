package uoa.se325.lab04.dto.concert;

import uoa.se325.lab04.model.concert.Concert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewConcertRequest {
    private Long id;
    private String title;
    private List<LocalDateTime> dates = new ArrayList<>();

    public NewConcertRequest() {
    }

    public NewConcertRequest(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public NewConcertRequest(String title, LocalDateTime... dates) {
        this.title = title;
        this.dates.addAll(Arrays.asList(dates));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<LocalDateTime> getDates() {
        return dates;
    }

    public void setDates(List<LocalDateTime> dates) {
        this.dates = dates;
    }

    /**
     * Converts this DTO instance to a domain instance.
     *
     * @return a {@link Concert}
     */
    public Concert toConcert() {
        var concert = new Concert(id, title);
        concert.getDates().addAll(dates);
        return concert;
    }
}
