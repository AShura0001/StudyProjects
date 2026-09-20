package uoa.se325.assignment01.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Describes one contiguous zone of seats on an {@link AircraftType}, all sharing the same
 * {@link CabinClass}. A zone spans a range of consecutive row numbers, and each row contains
 * one seat per valid seat letter.
 * <p>
 * For example, a zone with 2 rows starting at row 10, with valid letters "AJK", describes seats
 * 10A, 10J, 10K, 11A, 11J and 11K.
 */
@Embeddable
public class SeatingZone {

    @Enumerated(EnumType.STRING)
    private CabinClass cabinClass;

    private int numRows;

    private int startingRowNumber;

    private String seatCodeData;

    public SeatingZone() {
    }

    public SeatingZone(CabinClass cabinClass, int numRows, int startingRowNumber, String seatCodeData) {
        this.cabinClass = cabinClass;
        this.numRows = numRows;
        this.startingRowNumber = startingRowNumber;
        this.seatCodeData = seatCodeData;
    }

    public CabinClass getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(CabinClass cabinClass) {
        this.cabinClass = cabinClass;
    }

    public int getNumRows() {
        return numRows;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    public int getStartingRowNumber() {
        return startingRowNumber;
    }

    public void setStartingRowNumber(int startingRowNumber) {
        this.startingRowNumber = startingRowNumber;
    }

    public String getSeatCodeData() {
        return seatCodeData;
    }

    public void setSeatCodeData(String seatCodeData) {
        this.seatCodeData = seatCodeData;
    }
}
