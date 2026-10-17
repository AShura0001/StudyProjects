package lavs.business.repository;

import lavs.business.model.Draft;

import java.util.List;

/**
 * What {@link lavs.business.service.DraftSyncService} needs from the mainframe to
 * send a saved draft.
 *
 * <p>The business layer only knows about this interface, so tests can use a fake
 * and the data access layer decides how the mainframe is actually called.
 */
public interface DraftUploadGateway {

    /**
     * Sends one draft to the mainframe.
     *
     * <p>The draft's payload is passed on as it was saved. When the draft belongs
     * to other records, the real numbers the mainframe gave those records are
     * passed separately, so nothing inside the payload text has to be edited.
     *
     * <p>What each number means depends on the draft's type, and is agreed between
     * the code that creates the draft and the adapter that sends it. For example, a
     * loan has one (its customer) and a co-borrower has two (its loan, then the
     * co-borrower's own customer record).
     *
     * @param draft the draft to send
     * @param parentNumbers the mainframe's numbers for the records this draft
     *                      belongs to, in the same order as
     *                      {@link Draft#getDependsOn()}; empty when the draft does
     *                      not belong to another record, such as a customer
     * @return the number the mainframe assigned to the new record
     * @throws RecordRejectedException if the mainframe refused the record, with the
     *                                 reasons for each field
     * @throws MainframeUnavailableException if the mainframe could not be reached,
     *                                       so it is not known whether the record arrived
     */
    String upload(Draft draft, List<String> parentNumbers);
}
