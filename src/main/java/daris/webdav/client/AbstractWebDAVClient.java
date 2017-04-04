package daris.webdav.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import daris.util.PathUtils;
import daris.util.URLUtils;

public abstract class AbstractWebDAVClient implements WebDAVClient {

    private String _baseUri;
    private String _username;
    private String _password;
    private int _maxNumberOfRetries;
    private int _retryInterval;
    private int _maxNumberOfConnectionsPerUri;

    protected AbstractWebDAVClient(String baseUri, String username, String password, int maxNumberOfRetries, int retryInterval,
            int maxNumberOfConnectionsPerUri) {
        _baseUri = baseUri;
        _username = username;
        _password = password;
        _maxNumberOfRetries = maxNumberOfRetries;
        _retryInterval = retryInterval;
        _maxNumberOfConnectionsPerUri = maxNumberOfConnectionsPerUri;
    }

    @Override
    public String baseUri() {
        return _baseUri;
    }

    protected String username() {
        return _username;
    }

    protected String password() {
        return _password;
    }

    @Override
    public void mkcol(String path) throws Throwable {
        mkcol(path, maxNumberOfRetries());
    }

    protected abstract void mkcol(String path, int nbRetries) throws Throwable;

    @Override
    public void put(String path, File file) throws Throwable {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            put(path, in, file.length(), null);
        } finally {
            in.close();
        }
    }

    @Override
    public void put(String path, InputStream in, long length, String contentType) throws Throwable {
        put(path, in, length, contentType, null);
    }

    @Override
    public void put(String path, InputStream in, long length, String contentType, Map<String, String> headers)
            throws Throwable {
        put(path, in, length, contentType, headers, maxNumberOfRetries());
    }

    protected abstract void put(String path, InputStream in, long length, String contentType,
            Map<String, String> headers, int nbRetries) throws Throwable;

    /**
     * Number of retries.
     * 
     * @return
     */
    @Override
    public int maxNumberOfRetries() {
        return _maxNumberOfRetries;
    }

    /**
     * Retry interval in milliseconds.
     * 
     * @return
     */
    @Override
    public int retryInterval() {
        return _retryInterval;
    }

    @Override
    public int maxNumberOfConnectionPerUri() {
        return _maxNumberOfConnectionsPerUri;
    }

    protected String toUri(String path) throws Throwable {
        return toUri(baseUri(), path);
    }

    protected String serverHost() throws Throwable {
        return serverHost(baseUri());
    }

    public static String toUri(String baseUri, String path) throws Throwable {
        return PathUtils.join(baseUri, URLUtils.encode(path));
    }

    public static String serverHost(String baseUri) throws Throwable {
        return URI.create(baseUri).getHost();
    }

}
