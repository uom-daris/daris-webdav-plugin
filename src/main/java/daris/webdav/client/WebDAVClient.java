package daris.webdav.client;

import java.io.InputStream;
import java.util.Map;

public interface WebDAVClient {

    public static final int DEFAULT_MAX_NUMBER_OF_RETRIES = 255;
    public static final int DEFAULT_RETRY_INTERVAL = 500;

    String baseUrl();

    boolean exists(String path) throws Throwable;

    void mkcol(String path) throws Throwable;

    void put(String path, InputStream in, long length, String contentType, Map<String, String> headers)
            throws Throwable;

}
