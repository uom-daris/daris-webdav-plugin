package daris.webdav.client;

public class WebDAVClientFactory {

    public static WebDAVClient create(String baseUri, String username, String password, int maxNumberOfRetries,
            int retryInterval, int maxNumberOfConnectionsPerUri) {
        return new WebDAVClientImpl(baseUri, username, password, maxNumberOfRetries, retryInterval,
                maxNumberOfConnectionsPerUri);
    }
    
    public static WebDAVClient create(String baseUri, String username, String password) {
        return create(baseUri, username, password, WebDAVClient.DEFAULT_MAX_NUMBER_OF_RETRIES, WebDAVClient.DEFAULT_RETRY_INTERVAL,
                WebDAVClient.DEFAULT_MAX_NUMBER_OF_CONNECTIONS_PER_URI);
    }

}
