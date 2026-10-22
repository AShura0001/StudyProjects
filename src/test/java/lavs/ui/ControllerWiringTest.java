package lavs.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the rule that screens talk to the business layer, never to services of their own. */
class ControllerWiringTest {

    private static final Path UI = Path.of("src", "main", "java", "lavs", "ui");

    @Test
    void thereAreNoServicesInTheUiPackage() {
        assertTrue(Files.notExists(UI.resolve("service")), "lavs/ui/service should not exist");
        assertTrue(Files.notExists(UI.resolve("stub")), "lavs/ui/stub should not exist");
    }

    @Test
    void uiCodeDoesNotReachTheMainframeDirectly() throws IOException {
        try (Stream<Path> files = Files.walk(UI)) {
            List<String> offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            String code = Files.readString(p);
                            return code.contains("import lavs.ui.service") || code.contains("import lavs.ui.stub")
                                    || code.contains("import lavs.infrastructure.mainframe.Customer")
                                    || code.contains("import lavs.infrastructure.mainframe.Loan")
                                    || code.contains("import mainframe.client.messages.customer.")
                                    || code.contains("import mainframe.client.messages.loan.List")
                                    || code.contains("import mainframe.client.messages.loan.Retrieve");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(Path::toString)
                    .toList();
            assertEquals(List.of(), offenders);
        }
    }
}
