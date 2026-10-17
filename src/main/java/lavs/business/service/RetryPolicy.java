package lavs.business.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Decides how long to wait before trying again after the connection was lost.
 *
 * <p>The wait doubles with every failure in a row, up to a maximum, so a long
 * outage does not mean constant attempts. A little randomness (jitter) is added
 * so that many devices that lost the connection together do not all retry at the
 * same moment. The wait is always between half of the doubled value and all of
 * it, never close to zero.
 */
public class RetryPolicy {

    private static final Duration STANDARD_BASE = Duration.ofSeconds(1);
    private static final Duration STANDARD_MAX = Duration.ofSeconds(60);

    private final Duration base;
    private final Duration max;
    private final DoubleSupplier jitter;

    /**
     * Creates a policy.
     *
     * @param base the wait after the first failure, before jitter
     * @param max the longest the wait can ever be, before jitter
     * @param jitter supplies a number from 0 to 1 for each wait: 0 gives the
     *               shortest allowed wait and 1 the longest. Numbers outside that
     *               range are treated as the nearest end.
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the base is not positive or the maximum
     *                                  is shorter than the base
     */
    public RetryPolicy(Duration base, Duration max, DoubleSupplier jitter) {
        this.base = Objects.requireNonNull(base, "base must not be null");
        this.max = Objects.requireNonNull(max, "max must not be null");
        this.jitter = Objects.requireNonNull(jitter, "jitter must not be null");
        if (base.isNegative() || base.isZero()) {
            throw new IllegalArgumentException("base must be longer than zero");
        }
        if (max.compareTo(base) < 0) {
            throw new IllegalArgumentException("max must not be shorter than base");
        }
    }

    /**
     * Creates the policy the application uses: about a second after the first
     * failure, doubling up to about a minute, with random jitter.
     *
     * @return the standard policy
     */
    public static RetryPolicy standard() {
        return new RetryPolicy(STANDARD_BASE, STANDARD_MAX, () -> ThreadLocalRandom.current().nextDouble());
    }

    /**
     * Gets how long to wait before the next attempt.
     *
     * @param failures how many attempts in a row have failed, at least 1
     * @return the time to wait
     * @throws IllegalArgumentException if failures is less than 1
     */
    public Duration delayAfter(int failures) {
        if (failures < 1) {
            throw new IllegalArgumentException("failures must be at least 1");
        }
        Duration ceiling = base;
        for (int i = 1; i < failures && ceiling.compareTo(max) < 0; i++) {
            ceiling = ceiling.multipliedBy(2);
        }
        if (ceiling.compareTo(max) > 0) {
            ceiling = max;
        }
        Duration half = ceiling.dividedBy(2);
        double fraction = Math.max(0.0, Math.min(1.0, jitter.getAsDouble()));
        return half.plus(Duration.ofNanos((long) (half.toNanos() * fraction)));
    }
}
