package lavs.infrastructure.mainframe;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.ReferenceDataGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads the mainframe's constant categories and their values for
 * {@link lavs.business.service.ReferenceDataService}, fetching every page.
 *
 * <p>Constants are always there to read, so any trouble, including the mainframe
 * refusing the request or its reply not being readable, is reported as
 * {@link MainframeUnavailableException}. The service then tries again next time
 * instead of keeping an empty list.
 */
public class ReferenceDataAdapter implements ReferenceDataGateway {

    /** The message used when the constants could not be read. */
    public static final String UNAVAILABLE_MESSAGE =
            "The mainframe could not be reached or its reply could not be read.";

    private static final long PAGE_SIZE = 100;
    private static final int MAX_PAGES = 1000;

    private final Connection connection;

    /**
     * Creates an adapter that asks through the given connection, which must already be logged in.
     *
     * @param connection the connection to the mainframe
     * @throws NullPointerException if the connection is null
     */
    public ReferenceDataAdapter(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
    }

    @Override
    public List<ReferenceCategory> listCategories() {
        List<ReferenceCategory> categories = new ArrayList<>();
        for (long page = 1; page <= MAX_PAGES; page++) {
            mainframe.client.messages.constantCategory.List message =
                    new mainframe.client.messages.constantCategory.List(page, PAGE_SIZE);
            send(message);
            try {
                var items = message.getItems();
                for (var item : items) {
                    categories.add(new ReferenceCategory(item.getId(), item.getName()));
                }
                if (items.length == 0 || categories.size() >= message.getTotal()) {
                    return categories;
                }
            } catch (RuntimeException e) {
                throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE, e);
            }
        }
        return categories;
    }

    @Override
    public List<ReferenceItem> listItems(long categoryId) {
        List<ReferenceItem> values = new ArrayList<>();
        for (long page = 1; page <= MAX_PAGES; page++) {
            mainframe.client.messages.constantItem.List message =
                    new mainframe.client.messages.constantItem.List(categoryId, page, PAGE_SIZE);
            send(message);
            try {
                var items = message.getItems();
                for (var item : items) {
                    values.add(new ReferenceItem(item.getName(), item.getOrder()));
                }
                if (items.length == 0 || values.size() >= message.getTotal()) {
                    return values;
                }
            } catch (RuntimeException e) {
                throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE, e);
            }
        }
        return values;
    }

    private void send(Message message) {
        Status status;
        try {
            status = message.send(connection);
        } catch (IOException | RuntimeException e) {
            throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE, e);
        }
        MainframeCalls.requireSession(status);
        if (!status.getWasSuccessful()) {
            throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE);
        }
    }
}
