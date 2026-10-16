package lavs.ui.stub;

/**
 * Temporary stand-in for the business layer's AuthService.
 *
 * The screens are being built ahead of the services, so the controllers talk to
 * a stub with the shape we expect the real service to have. When AuthService
 * lands, the controller swaps this type for that one and nothing else about the
 * view changes.
 *
 * Deliberately blocking: the controller is responsible for keeping the call off
 * the JavaFX thread, and a stub that returns instantly would hide a mistake there.
 */
public class StubAuthService {

    /** Mirrors what the real service is expected to hand back. */
    public record Result(boolean success, String message) {
        public static Result ok() {
            return new Result(true, null);
        }

        public static Result failure(String message) {
            return new Result(false, message);
        }
    }

    private static final String DEMO_EMPLOYEE_ID = "admin";
    private static final String DEMO_PASSWORD = "admin";

    private final long simulatedLatencyMillis;

    public StubAuthService() {
        this(700);
    }

    public StubAuthService(long simulatedLatencyMillis) {
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    public Result login(String employeeId, String password) {
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure("Sign in was interrupted.");
        }

        if (DEMO_EMPLOYEE_ID.equals(employeeId) && DEMO_PASSWORD.equals(password)) {
            return Result.ok();
        }
        return Result.failure("Employee ID or password is incorrect.");
    }
}
