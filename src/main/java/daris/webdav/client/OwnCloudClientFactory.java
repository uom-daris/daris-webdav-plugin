package daris.webdav.client;

public class OwnCloudClientFactory {

    public static OwnCloudClient create(String baseUri, String username, String password, int maxNumberOfRetries,
            int retryInterval, int maxNumberOfConnectionsPerUri, long chunkSize) {
        return new OwnCloudClientImpl(baseUri, username, password, maxNumberOfRetries, retryInterval,
                maxNumberOfConnectionsPerUri, chunkSize);
    }

    public static OwnCloudClient create(String baseUri, String username, String password, long chunkSize) {
        return create(baseUri, username, password, OwnCloudClient.DEFAULT_MAX_NUMBER_OF_RETRIES,
                OwnCloudClient.DEFAULT_RETRY_INTERVAL, OwnCloudClient.DEFAULT_MAX_NUMBER_OF_CONNECTIONS_PER_URI,
                chunkSize);
    }

    public static OwnCloudClient create(String baseUri, String username, String password) {
        return create(baseUri, username, password);
    }

}
