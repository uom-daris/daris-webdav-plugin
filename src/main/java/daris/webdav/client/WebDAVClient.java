package daris.webdav.client;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public interface WebDAVClient extends HttpClient {

    public static final int DEFAULT_MAX_NUMBER_OF_RETRIES = 255;
    public static final int DEFAULT_RETRY_INTERVAL = 500;
    public static final int DEFAULT_MAX_NUMBER_OF_CONNECTIONS_PER_URI = 0;

    String baseUri();

    boolean exists(String path) throws Throwable;

    void mkcol(String path) throws Throwable;

    void put(String path, File in) throws Throwable;

    void put(String path, InputStream in, long length, String contentType) throws Throwable;

    void put(String path, InputStream in, long length, String contentType, Map<String, String> headers)
            throws Throwable;

    int maxNumberOfRetries();

    int retryInterval();

    int maxNumberOfConnectionPerUri();

}
