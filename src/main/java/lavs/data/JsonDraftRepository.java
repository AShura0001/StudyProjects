package lavs.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.DraftStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stores each draft as its own JSON file inside one folder.
 *
 * <p>A save writes the new version to a temporary file first and then renames it
 * over the real file, so a crash part way through never leaves a half-written
 * draft behind.
 */
public class JsonDraftRepository implements DraftRepository {

    private static final String FILE_EXTENSION = ".json";
    private static final String TEMP_EXTENSION = ".json.tmp";

    private final Path folder;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a repository that keeps its drafts in the given folder. The folder
     * is created on the first save if it does not exist.
     *
     * @param folder the folder that holds the draft files
     * @throws NullPointerException if the folder is null
     */
    public JsonDraftRepository(Path folder) {
        this.folder = Objects.requireNonNull(folder, "folder must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>The draft is written to a temporary file and then renamed in a single
     * step, so the previous version stays intact until the new one is complete.
     */
    @Override
    public void save(Draft draft) {
        Objects.requireNonNull(draft, "draft must not be null");
        Path target = folder.resolve(draft.getLocalId() + FILE_EXTENSION);
        Path temp = folder.resolve(draft.getLocalId() + TEMP_EXTENSION);
        try {
            Files.createDirectories(folder);
            mapper.writeValue(temp.toFile(), toFile(draft));
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new DraftStorageException("Could not save draft " + draft.getLocalId(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A missing file is not an error and gives an empty result. A file that
     * exists but cannot be understood is reported with a
     * {@link DraftStorageException}, so a damaged draft is never mistaken for a
     * missing one.
     */
    @Override
    public Optional<Draft> findById(String localId) {
        Objects.requireNonNull(localId, "localId must not be null");
        Path file = folder.resolve(localId + FILE_EXTENSION);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            DraftFile read = mapper.readValue(file.toFile(), DraftFile.class);
            return Optional.of(toDraft(read));
        } catch (IOException | IllegalArgumentException | DateTimeException e) {
            throw new DraftStorageException("Could not read draft " + localId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Draft> findPending() {
        throw new UnsupportedOperationException("not done yet");
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String localId) {
        throw new UnsupportedOperationException("not done yet");
    }

    private DraftFile toFile(Draft draft) {
        return new DraftFile(
                draft.getLocalId(),
                draft.getType().name(),
                draft.getPayload(),
                draft.getStatus().name(),
                draft.getCreatedAt().toString());
    }

    private Draft toDraft(DraftFile file) {
        if (file.localId() == null || file.type() == null || file.payload() == null
                || file.status() == null || file.createdAt() == null) {
            throw new IllegalArgumentException("draft file is missing a field");
        }
        return new Draft(
                file.localId(),
                DraftType.valueOf(file.type()),
                file.payload(),
                DraftStatus.valueOf(file.status()),
                Instant.parse(file.createdAt()));
    }
}
