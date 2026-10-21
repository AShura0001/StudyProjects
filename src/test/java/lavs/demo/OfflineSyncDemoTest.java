package lavs.demo;

import lavs.infrastructure.mainframe.ScriptedConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineSyncDemoTest {

    private final ScriptedConnection mainframe = new ScriptedConnection();
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    private OfflineSyncDemo demo(OfflineSyncDemo.Pause pause) {
        return new OfflineSyncDemo(mainframe, "01-0001-0000020",
                new PrintStream(captured, true, StandardCharsets.UTF_8), pause);
    }

    private void loanReply() {
        mainframe.replySuccess("loanNumber", "LN-DEMO", "customerNumber", "01-0001-0000020", "rateType", "Fixed",
                "rateAmount", "6.5", "rateFrequency", "Yearly", "paymentAmount", "500.0",
                "paymentFrequency", "Monthly", "principal", "25000.0", "startDate", "2026-10-19",
                "renewalDate", "2031-10-19", "status", "New");
    }

    @Test
    void run_savesALoanOfflineKeepsItThroughARestartAndSendsItOnceWhenTheSignalReturns() throws Exception {
        loanReply();

        demo(OfflineSyncDemo.Pause.NONE).run();

        assertTrue(output().contains("saved locally, not yet sent"));
        assertTrue(output().contains("The loan survived the restart. Status: PENDING"));
        assertTrue(output().contains("SYNCED"));
        assertTrue(output().contains("LN-DEMO"));
        assertTrue(output().contains("No duplicate"));
        assertEquals(1, mainframe.requests().size());
        assertEquals("loan.create", mainframe.requests().get(0).getRequestType());
    }

    @Test
    void run_sendsNothingWhileTheSignalIsOff() throws Exception {
        loanReply();
        List<String> promptsSeen = new ArrayList<>();
        OfflineSyncDemo.Pause recording = prompt -> {
            promptsSeen.add(prompt);
            if (prompt.startsWith("Step 3")) {
                assertTrue(mainframe.requests().isEmpty(), "nothing may be sent before the signal returns");
            }
        };

        demo(recording).run();

        assertEquals(3, promptsSeen.size());
        assertTrue(promptsSeen.get(0).startsWith("Step 1"));
        assertTrue(promptsSeen.get(1).startsWith("Step 2"));
    }

    @Test
    void pause_none_neverWaits() {
        OfflineSyncDemo.Pause.NONE.await("anything");
    }
}
