package lavs.data;

import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * On Windows a file cannot be replaced while something else has it open (a virus
 * scanner, an indexer, another reader). Saving must ride out a short lock like that
 * instead of failing the sync pass.
 */
class JsonDraftRepositoryBusyFileTest {

    @TempDir
    Path folder;

    @Test
    void save_whileAnotherReaderBrieflyHasTheFileOpen_stillSucceeds() throws Exception {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Draft draft = new Draft(DraftType.LOAN, "first");
        repository.save(draft);
        Path file = folder.resolve(draft.getLocalId() + ".json");

        CountDownLatch opened = new CountDownLatch(1);
        Thread reader = new Thread(() -> {
            try (InputStream in = Files.newInputStream(file)) {
                in.read();
                opened.countDown();
                Thread.sleep(150); // hold the file open while the save happens
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        reader.start();
        assertTrue(opened.await(2, TimeUnit.SECONDS));

        draft.revise("second");
        repository.save(draft);
        reader.join();

        assertEquals("second", new JsonDraftRepository(folder).findById(draft.getLocalId()).orElseThrow().getPayload());
    }
}
