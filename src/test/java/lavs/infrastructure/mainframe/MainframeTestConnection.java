package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

public class MainframeTestConnection implements Connection {

    private final Queue<MessageResponse> responses =
            new ArrayDeque<>();

    private final List<MessageRequest> requests =
            new ArrayList<>();

    public void addResponse(MessageResponse response) {
        responses.add(response);
    }

    public List<MessageRequest> getRequests() {
        return List.copyOf(requests);
    }

    public MessageRequest getLastRequest() {
        if (requests.isEmpty()) {
            return null;
        }

        return requests.get(requests.size() - 1);
    }

    public int getRequestCount() {
        return requests.size();
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        requests.add(request);

        MessageResponse response = responses.poll();

        if (response == null) {
            throw new AssertionError(
                    "No response configured for request: "
                            + request.getRequestType()
            );
        }

        return response;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public Status Login(String username, String password) {
        return successStatus();
    }

    @Override
    public void Logout() {
    }

    public static MessageResponse successResponse(
            HashMap<String, String> values
    ) {
        return new MessageResponse(
                successStatus(),
                values
        );
    }

    public static MessageResponse failedResponse() {
        return new MessageResponse(
                new Status(1, "failed", 1),
                new HashMap<>()
        );
    }

    public static Status successStatus() {
        return new Status(1);
    }

    public static HashMap<String, String> values(
            String... keyValuePairs
    ) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Values must contain key/value pairs"
            );
        }

        HashMap<String, String> values = new HashMap<>();

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            values.put(
                    keyValuePairs[i],
                    keyValuePairs[i + 1]
            );
        }

        return values;
    }

    public static MessageResponse successResponse(
            String... keyValuePairs
    ) {
        return successResponse(values(keyValuePairs));
    }
}