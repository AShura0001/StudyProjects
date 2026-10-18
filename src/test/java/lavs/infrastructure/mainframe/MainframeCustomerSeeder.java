package lavs.infrastructure.mainframe;

import lavs.business.model.Document;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.Status;
import mainframe.client.messages.customer.AddDocument;
import mainframe.client.messages.customer.Create;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class MainframeCustomerSeeder {
    static Connection connection;

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

        connection = new HttpConnection(url);
        connection.Login(username, password);

        Document document = new Document();
        document.setCustomerNumber("01-0001-0000020");
        document.setFileName("proof-of-address.pdf");
        document.setFileType("application/pdf");
        document.setId(1001L);
        document.setNumber("DOC-1001");
        document.setSize(245760L);
        document.setType("ProofOfAddress");

        CustomerDocumentAdapter adapter = new CustomerDocumentAdapter(connection);
        for (int i = 0; i < 5; i++) {
            adapter.addCustomerDocument(document);
        }

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


}

