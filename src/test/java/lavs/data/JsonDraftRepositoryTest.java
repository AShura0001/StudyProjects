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
import java.util.List;
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

    @Test
    void findPending_whenFolderDoesNotExist_returnsEmptyList() {
        JsonDraftRepository repository = new JsonDraftRepository(folder.resolve("never-created"));

        assertTrue(repository.findPending().isEmpty());
    }

    @Test
    void findPending_withMixedStatuses_returnsOnlyPendingDrafts() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Instant time = Instant.parse("2026-10-01T09:30:00Z");
        repository.save(new Draft("pending", DraftType.LOAN, "a", DraftStatus.PENDING, time));
        repository.save(new Draft("synced", DraftType.LOAN, "b", DraftStatus.SYNCED, time));
        repository.save(new Draft("rejected", DraftType.LOAN, "c", DraftStatus.REJECTED, time));

        List<Draft> pending = repository.findPending();

        assertEquals(1, pending.size());
        assertEquals("pending", pending.get(0).getLocalId());
    }

    @Test
    void findPending_withSeveralDrafts_returnsOldestFirst() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        repository.save(new Draft("newest", DraftType.LOAN, "x", DraftStatus.PENDING,
                Instant.parse("2026-10-03T09:00:00Z")));
        repository.save(new Draft("oldest", DraftType.CUSTOMER, "x", DraftStatus.PENDING,
                Instant.parse("2026-10-01T09:00:00Z")));
        repository.save(new Draft("middle", DraftType.COBORROWER, "x", DraftStatus.PENDING,
                Instant.parse("2026-10-02T09:00:00Z")));

        List<String> order = repository.findPending().stream().map(Draft::getLocalId).toList();

        assertEquals(List.of("oldest", "middle", "newest"), order);
    }

    @Test
    void findPending_withSameCreationTime_ordersByIdSoTheOrderIsStable() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Instant time = Instant.parse("2026-10-01T09:00:00Z");
        repository.save(new Draft("b", DraftType.LOAN, "x", DraftStatus.PENDING, time));
        repository.save(new Draft("a", DraftType.LOAN, "x", DraftStatus.PENDING, time));

        List<String> order = repository.findPending().stream().map(Draft::getLocalId).toList();

        assertEquals(List.of("a", "b"), order);
    }

    @Test
    void findPending_withOneDamagedFile_skipsItAndReturnsTheOthers() throws IOException {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        repository.save(new Draft("good", DraftType.LOAN, "x", DraftStatus.PENDING,
                Instant.parse("2026-10-01T09:00:00Z")));
        Files.writeString(folder.resolve("broken.json"), "this is not json");

        List<Draft> pending = repository.findPending();

        assertEquals(1, pending.size());
        assertEquals("good", pending.get(0).getLocalId());
        assertTrue(Files.exists(folder.resolve("broken.json")));
    }

    @Test
    void findPending_withLeftoverTemporaryFile_ignoresIt() throws IOException {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Files.writeString(folder.resolve("half-written.json.tmp"), "{\"localId\":\"half");

        assertTrue(repository.findPending().isEmpty());
    }

    @Test
    void delete_withExistingDraft_removesIt() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Draft draft = new Draft(DraftType.LOAN, "x");
        repository.save(draft);

        repository.delete(draft.getLocalId());

        assertTrue(repository.findById(draft.getLocalId()).isEmpty());
    }

    @Test
    void delete_withUnknownId_doesNothing() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);

        repository.delete("does-not-exist");

        assertFalse(Files.exists(folder.resolve("does-not-exist.json")));
    }

    @Test
    void delete_ofOneDraft_leavesOtherDraftsAlone() {
        JsonDraftRepository repository = new JsonDraftRepository(folder);
        Draft keep = new Draft(DraftType.CUSTOMER, "keep");
        Draft remove = new Draft(DraftType.LOAN, "remove");
        repository.save(keep);
        repository.save(remove);

        repository.delete(remove.getLocalId());

        assertTrue(repository.findById(keep.getLocalId()).isPresent());
    }
}
