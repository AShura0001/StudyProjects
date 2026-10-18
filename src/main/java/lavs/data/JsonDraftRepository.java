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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Stores each draft as its own JSON file inside one folder.
 *
 * <p>A save writes the new version to a temporary file first and then renames it
 * over the real file, so a crash part way through never leaves a half-written
 * draft behind.
 *
 * <p>It is safe to use from several threads at once, for example the screen
 * saving a new draft while the background sync reads and updates others. Calls
 * take turns, and none of them waits for the network.
 */
public class JsonDraftRepository implements DraftRepository {

    private static final Logger LOGGER = Logger.getLogger(JsonDraftRepository.class.getName());
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
    public synchronized void save(Draft draft) {
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
    public synchronized Optional<Draft> findById(String localId) {
        Objects.requireNonNull(localId, "localId must not be null");
        Path file = folder.resolve(localId + FILE_EXTENSION);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(read(file));
    }

    /**
     * {@inheritDoc}
     *
     * <p>A file that cannot be read is skipped and a warning is logged, so one
     * damaged file does not hold up every other draft. The damaged file is left
     * on disk untouched. Temporary files left by an interrupted save are ignored.
     * Drafts with the same creation time are ordered by ID so the order is stable.
     */
    @Override
    public synchronized List<Draft> findPending() {
        return findByStatus(DraftStatus.PENDING);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A file that cannot be read is skipped and a warning is logged. The damaged
     * file is left on disk untouched. Temporary files left by an interrupted save
     * are ignored. Drafts with the same creation time are ordered by ID so the
     * order is stable.
     */
    @Override
    public synchronized List<Draft> findByStatus(DraftStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        List<Draft> matching = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (!file.getFileName().toString().endsWith(FILE_EXTENSION)) {
                    continue;
                }
                try {
                    Draft draft = read(file);
                    if (draft.getStatus() == status) {
                        matching.add(draft);
                    }
                } catch (DraftStorageException e) {
                    LOGGER.log(Level.WARNING, "Skipping unreadable draft file " + file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            throw new DraftStorageException("Could not list drafts in " + folder, e);
        }
        matching.sort(Comparator.comparing(Draft::getCreatedAt).thenComparing(Draft::getLocalId));
        return matching;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void delete(String localId) {
        Objects.requireNonNull(localId, "localId must not be null");
        try {
            Files.deleteIfExists(folder.resolve(localId + FILE_EXTENSION));
        } catch (IOException e) {
            throw new DraftStorageException("Could not delete draft " + localId, e);
        }
    }

    private Draft read(Path file) {
        try {
            return toDraft(mapper.readValue(file.toFile(), DraftFile.class));
        } catch (IOException | IllegalArgumentException | DateTimeException e) {
            throw new DraftStorageException("Could not read draft file " + file.getFileName(), e);
        }
    }

    private DraftFile toFile(Draft draft) {
        return new DraftFile(
                draft.getLocalId(),
                draft.getType().name(),
                draft.getPayload(),
                draft.getStatus().name(),
                draft.getCreatedAt().toString(),
                draft.getServerNumber().orElse(null),
                draft.getErrors(),
                draft.getDependsOn());
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
                Instant.parse(file.createdAt()),
                file.serverNumber(),
                file.errors() == null ? Map.of() : file.errors(),
                file.dependsOn() == null ? List.of() : file.dependsOn());
    }
}
