package lavs.business.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {

    private static final Duration ONE_SECOND = Duration.ofSeconds(1);
    private static final Duration ONE_MINUTE = Duration.ofSeconds(60);

    private static RetryPolicy withJitter(double jitter) {
        return new RetryPolicy(ONE_SECOND, ONE_MINUTE, () -> jitter);
    }

    @Test
    void delayAfter_withFullJitter_doublesEachTimeUntilTheMaximum() {
        RetryPolicy policy = withJitter(1.0);

        List<Duration> delays = List.of(1, 2, 3, 4, 5, 6, 7, 8).stream().map(policy::delayAfter).toList();

        assertEquals(List.of(1L, 2L, 4L, 8L, 16L, 32L, 60L, 60L),
                delays.stream().map(Duration::toSeconds).toList());
    }

    @Test
    void delayAfter_withNoJitter_waitsHalfOfTheDoubledValue() {
        RetryPolicy policy = withJitter(0.0);

        assertEquals(Duration.ofMillis(500), policy.delayAfter(1));
        assertEquals(Duration.ofSeconds(1), policy.delayAfter(2));
        assertEquals(Duration.ofSeconds(30), policy.delayAfter(50));
    }

    @Test
    void delayAfter_withJitterOutsideZeroToOne_usesTheNearestEnd() {
        assertEquals(Duration.ofSeconds(2), withJitter(7.5).delayAfter(2));
        assertEquals(Duration.ofSeconds(1), withJitter(-3.0).delayAfter(2));
    }

    @Test
    void delayAfter_withAHugeNumberOfFailures_staysAtTheMaximum() {
        assertEquals(ONE_MINUTE, withJitter(1.0).delayAfter(Integer.MAX_VALUE));
    }

    @Test
    void delayAfter_withZeroOrNegativeFailures_isRejected() {
        RetryPolicy policy = withJitter(1.0);

        assertThrows(IllegalArgumentException.class, () -> policy.delayAfter(0));
        assertThrows(IllegalArgumentException.class, () -> policy.delayAfter(-1));
    }

    @Test
    void standard_afterTheFirstFailure_waitsBetweenHalfASecondAndASecond() {
        Duration delay = RetryPolicy.standard().delayAfter(1);

        assertTrue(delay.compareTo(Duration.ofMillis(500)) >= 0);
        assertTrue(delay.compareTo(ONE_SECOND) <= 0);
    }

    @Test
    void standard_afterManyFailures_neverWaitsMoreThanAMinute() {
        for (int failures = 1; failures <= 20; failures++) {
            assertTrue(RetryPolicy.standard().delayAfter(failures).compareTo(ONE_MINUTE) <= 0);
        }
    }

    @Test
    void newPolicy_withAZeroBase_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryPolicy(Duration.ZERO, ONE_MINUTE, () -> 0.5));
    }

    @Test
    void newPolicy_withANegativeBase_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryPolicy(Duration.ofSeconds(-1), ONE_MINUTE, () -> 0.5));
    }

    @Test
    void newPolicy_withAMaximumShorterThanTheBase_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryPolicy(ONE_MINUTE, ONE_SECOND, () -> 0.5));
    }

    @Test
    void newPolicy_withNullArguments_isRejected() {
        assertThrows(NullPointerException.class, () -> new RetryPolicy(null, ONE_MINUTE, () -> 0.5));
        assertThrows(NullPointerException.class, () -> new RetryPolicy(ONE_SECOND, null, () -> 0.5));
        assertThrows(NullPointerException.class, () -> new RetryPolicy(ONE_SECOND, ONE_MINUTE, null));
    }
}
