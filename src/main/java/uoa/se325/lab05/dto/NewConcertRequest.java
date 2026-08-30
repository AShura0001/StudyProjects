package uoa.se325.lab05.dto;

import uoa.se325.lab05.model.Concert;

import java.time.LocalDateTime;

public class NewConcertRequest {
    private String title;
    private LocalDateTime startTime;
    private int durationInMinutes;
    private int standardTicketPrice;

    public NewConcertRequest() {
    }

    public NewConcertRequest(String title, LocalDateTime startTime, int durationInMinutes, int standardTicketPrice) {
        this.title = title;
        this.startTime = startTime;
        this.durationInMinutes = durationInMinutes;
        this.standardTicketPrice = standardTicketPrice;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(int durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public int getStandardTicketPrice() {
        return standardTicketPrice;
    }

    public void setStandardTicketPrice(int standardTicketPrice) {
        this.standardTicketPrice = standardTicketPrice;
    }

    /**
     * Converts this DTO instance to a domain instance.
     *
     * @return a {@link Concert}
     */
    public Concert toConcert() {
        return new Concert(title, startTime, durationInMinutes, standardTicketPrice);
    }
}
