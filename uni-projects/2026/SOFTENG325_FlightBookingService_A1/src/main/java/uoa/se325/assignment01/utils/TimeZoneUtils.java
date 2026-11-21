package uoa.se325.assignment01.utils;

import java.time.*;

public class TimeZoneUtils {
    /**
     * Parses the given departure date query. If the query matches the format "YYYY-MM-DD" (e.g. "2021-08-16"), an array
     * of two {@link ZonedDateTime} instances corresponding to 00:00:00 and 23:59:59 on the given date in the given
     * timezone is returned. If dayRange is > 0, the range is expanded by (24 * dayRange) hours on either side.
     *
     * @param baseDate the local date to start with
     * @param dayRange the range, in days. Adds (24 * dayRange) hours on each side of the range to search.
     * @param timezone the timezone to parse.
     * @return an array of two {@link ZonedDateTime} instances, representing the beginning and end of the given date
     * in the given timezone
     * @throws DateTimeException if baseDate or timezone are invalid
     */
    public static ZonedDateTime[] toZonedDateRange(LocalDate baseDate, int dayRange, String timezone) throws DateTimeException {
        LocalDate minDate = baseDate.minusDays(dayRange);
        LocalDate maxDate = baseDate.plusDays(dayRange);
        return new ZonedDateTime[]{
                ZonedDateTime.of(minDate, LocalTime.MIN, ZoneId.of(timezone)),
                ZonedDateTime.of(maxDate, LocalTime.MAX, ZoneId.of(timezone))
        };
    }
}
