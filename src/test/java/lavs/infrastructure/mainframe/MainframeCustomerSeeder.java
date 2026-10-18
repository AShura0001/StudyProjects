package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.Status;
import mainframe.client.messages.customer.Create;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class MainframeCustomerSeeder {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private MainframeCustomerSeeder() {
    }

    public static void main(String[] args) throws IOException {
        int numberOfCustomers = args.length > 0
                ? Integer.parseInt(args[0])
                : 5;

        String url = getSetting(
                "MAINFRAME_URL",
                "mainframe.url",
                "https://animating-eraser-arrogance.ngrok-free.dev/"
        );

        String username = getSetting(
                "MAINFRAME_USERNAME",
                "mainframe.username",
                "Group_2"

        );

        String password = getSetting(
                "MAINFRAME_PASSWORD",
                "mainframe.password",
                "bug"
        );

        List<SeededCustomer> customers = seedCustomers(
                url,
                username,
                password,
                numberOfCustomers
        );

        System.out.println();
        System.out.println("Created " + customers.size() + " customers:");

        for (SeededCustomer customer : customers) {
            System.out.println(
                    customer.number()
                            + " - "
                            + customer.firstName()
                            + " "
                            + customer.familyName()
            );
        }
    }

    public static List<SeededCustomer> seedCustomers(
            String url,
            String username,
            String password,
            int numberOfCustomers
    ) throws IOException {

        if (numberOfCustomers < 1) {
            throw new IllegalArgumentException(
                    "numberOfCustomers must be greater than zero"
            );
        }

        Connection connection = new HttpConnection(url);
        List<SeededCustomer> createdCustomers = new ArrayList<>();

        try {
            Status loginStatus = connection.Login(username, password);

            if (!loginStatus.getWasSuccessful()) {
                throw new IOException(
                        "Mainframe login failed: "
                                + loginStatus.getErrorMessage()
                );
            }

            for (int i = 1; i <= numberOfCustomers; i++) {
                Create create = buildCustomer(i);

                Status createStatus = create.send(connection);

                if (!createStatus.getWasSuccessful()) {
                    throw new IOException(
                            "Could not create customer " + i + ": "
                                    + createStatus.getErrorMessage()
                    );
                }

                createdCustomers.add(new SeededCustomer(
                        create.getNumber(),
                        create.getFirstName(),
                        create.getFamilyName(),
                        create.getStatus()
                ));
            }

            return List.copyOf(createdCustomers);
        } finally {
            connection.Logout();
            connection.close();
        }
    }

    private static Create buildCustomer(int index) {
        String uniqueValue = String.valueOf(System.currentTimeMillis());

        return new Create(
                LocalDate.of(1990, 1, 1)
                        .plusDays(index)
                        .format(DATE_FORMAT),
                "IntegrationFamily" + index,
                "IntegrationFirst" + uniqueValue + "_" + index,
                "New Zealand",
                0,
                "Mr"
        );
    }

    private static String getRequiredSetting(
            String environmentVariable,
            String systemProperty
    ) {
        String value = getSetting(
                environmentVariable,
                systemProperty,
                null
        );

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing " + environmentVariable
                            + " environment variable or -D"
                            + systemProperty
                            + " system property"
            );
        }

        return value;
    }

    private static String getSetting(
            String environmentVariable,
            String systemProperty,
            String defaultValue
    ) {
        String systemPropertyValue = System.getProperty(systemProperty);

        if (systemPropertyValue != null
                && !systemPropertyValue.isBlank()) {
            return systemPropertyValue;
        }

        String environmentValue = System.getenv(environmentVariable);

        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return defaultValue;
    }

    public record SeededCustomer(
            String number,
            String firstName,
            String familyName,
            String status
    ) {
    }
}

