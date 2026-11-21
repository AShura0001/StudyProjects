package lavs.business.repository;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;

import java.util.List;

/**
 * What {@link lavs.business.service.ReferenceDataService} needs from the
 * mainframe to read constant values, such as address types and rate types.
 *
 * <p>Each method returns the complete list. If the mainframe sends results in
 * pages, the implementation fetches every page before returning.
 */
public interface ReferenceDataGateway {

    /**
     * Lists every constant category on the mainframe.
     *
     * @return all categories, in no particular order
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    List<ReferenceCategory> listCategories();

    /**
     * Lists every value in one category.
     *
     * @param categoryId the mainframe's ID for the category
     * @return all values in the category, in no particular order
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    List<ReferenceItem> listItems(long categoryId);
}
