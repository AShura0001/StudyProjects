package lavs.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonDraftRepositoryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path folder;

    @Test
    void save_withNewDraft_writesAJsonFileNamedAfterTheId() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Draft draft = new Draft(DraftType.LOAN, "loan text");

        repository.save(draft);

        assertTrue(Files.exists(folder.resolve(draft.getLocalId() + ".json")));
    }

    @Test
    void save_withNewDraft_writesAllFieldsToTheFile() throws IOException {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Instant created = Instant.parse("2026-10-01T09:30:00Z");
        Draft draft = new Draft("abc", DraftType.CUSTOMER, "customer text", DraftStatus.PENDING, created);

        repository.save(draft);

        DraftFile written = mapper.readValue(folder.resolve("abc.json").toFile(), DraftFile.class);
        assertEquals(new DraftFile("abc", "CUSTOMER", "customer text", "PENDING", "2026-10-01T09:30:00Z"), written);
    }

    @Test
    void save_calledTwiceForTheSameDraft_keepsTheLatestVersion() throws IOException {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Draft draft = new Draft(DraftType.LOAN, "loan text");
        repository.save(draft);

        draft.setStatus(DraftStatus.SYNCED);
        repository.save(draft);

        DraftFile written = mapper.readValue(
                folder.resolve(draft.getLocalId() + ".json").toFile(), DraftFile.class);
        assertEquals("SYNCED", written.status());
    }

    @Test
    void save_whenFinished_leavesNoTemporaryFileBehind() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Draft draft = new Draft(DraftType.LOAN, "loan text");

        repository.save(draft);

        assertFalse(Files.exists(folder.resolve(draft.getLocalId() + ".json.tmp")));
    }

    @Test
    void save_whenFolderDoesNotExistYet_createsTheFolder() {
        Path missing = folder.resolve("drafts");
        JsonDraftRepository repository = new JsonDraftRepository(missing);
        Draft draft = new Draft(DraftType.LOAN, "loan text");

        repository.save(draft);

        assertTrue(Files.exists(missing.resolve(draft.getLocalId() + ".json")));
    }

    @Test
    void findById_afterSave_returnsTheSameDraft() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Instant created = Instant.parse("2026-10-01T09:30:00Z");
        Draft saved = new Draft("abc", DraftType.CUSTOMER, "customer text", DraftStatus.REJECTED, created);
        repository.save(saved);

        Draft found = repository.findById("abc").orElseThrow();

        assertAll(
                () -> assertEquals("abc", found.getLocalId()),
                () -> assertEquals(DraftType.CUSTOMER, found.getType()),
                () -> assertEquals("customer text", found.getPayload()),
                () -> assertEquals(DraftStatus.REJECTED, found.getStatus()),
                () -> assertEquals(created, found.getCreatedAt()));
    }

    @Test
    void findById_withUnknownId_returnsEmpty() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);

        Optional<Draft> found = repository.findById("does-not-exist");

        assertTrue(found.isEmpty());
    }

    @Test
    void findById_withFileThatIsNotJson_throwsStorageException() throws IOException {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Files.writeString(folder.resolve("bad.json"), "this is not json");

        assertThrows(DraftStorageException.class, () -> repository.findById("bad"));
    }

    @Test
    void findById_withUnknownTypeInFile_throwsStorageException() throws IOException {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Files.writeString(folder.resolve("odd.json"),
                "{\"localId\":\"odd\",\"type\":\"BANANA\",\"payload\":\"x\","
                        + "\"status\":\"PENDING\",\"createdAt\":\"2026-10-01T09:30:00Z\"}");

        assertThrows(DraftStorageException.class, () -> repository.findById("odd"));
    }

    @Test
    void findById_withFieldMissingFromFile_throwsStorageException() throws IOException {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Files.writeString(folder.resolve("short.json"), "{\"localId\":\"short\",\"type\":\"LOAN\"}");

        assertThrows(DraftStorageException.class, () -> repository.findById("short"));
    }
}
