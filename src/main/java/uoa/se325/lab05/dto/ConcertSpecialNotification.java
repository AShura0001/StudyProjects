package uoa.se325.lab05.dto;

import java.util.Objects;

public class ConcertSpecialNotification {

    private long concertId;

    private String title;

    private int specialTicketPrice;

    public ConcertSpecialNotification(){}

    public ConcertSpecialNotification(long concertId, String title, int specialTicketPrice) {
        this.concertId = concertId;
        this.title = title;
        this.specialTicketPrice = specialTicketPrice;
    }

    public long getConcertId() {
        return concertId;
    }

    public void setConcertId(long concertId) {
        this.concertId = concertId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSpecialTicketPrice() {
        return specialTicketPrice;
    }

    public void setSpecialTicketPrice(int specialTicketPrice) {
        this.specialTicketPrice = specialTicketPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcertSpecialNotification that = (ConcertSpecialNotification) o;
        return concertId == that.concertId && specialTicketPrice == that.specialTicketPrice && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(concertId, title, specialTicketPrice);
    }
}
