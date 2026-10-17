package lavs.data;

import java.util.Map;

/**
 * The shape of one draft file on disk.
 *
 * <p>This is only a description of what the JSON looks like, so Jackson can
 * write it and read it back. It holds no rules. The enums and the creation time
 * are kept as text so the file is easy to read and needs no extra Jackson
 * modules. {@link JsonDraftRepository} converts between this and a
 * {@link lavs.business.model.Draft}.
 *
 * @param localId the unique ID of the draft
 * @param type the name of the draft type, for example {@code LOAN}
 * @param payload the record as plain text
 * @param status the name of the draft status, for example {@code PENDING}
 * @param createdAt the creation time as ISO-8601 text, for example {@code 2026-10-15T09:30:00Z}
 * @param serverNumber the number the mainframe assigned, or null if it has none
 * @param errors the reason for each refused field; may be missing in files saved
 *               before errors were stored, which is read as no errors
 */
public record DraftFile(
        String localId,
        String type,
        String payload,
        String status,
        String createdAt,
        String serverNumber,
        Map<String, String> errors) {
}
