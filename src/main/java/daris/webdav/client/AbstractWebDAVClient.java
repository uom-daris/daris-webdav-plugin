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

    private String _baseUrl;
    private String _username;
    private String _password;
    private int _maxRetries;
    private int _retryInterval;

    protected AbstractWebDAVClient(String baseUrl, String username, String password) {
        _baseUrl = baseUrl;
        _username = username;
        _password = password;
        _maxRetries = DEFAULT_MAX_NUMBER_OF_RETRIES;
        _retryInterval = DEFAULT_RETRY_INTERVAL;
    }

    @Override
    public String baseUrl() {
        return _baseUrl;
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

    public void put(String path, File file) throws Throwable {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            put(path, in, file.length(), null);
        } finally {
            in.close();
        }
    }

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
    public int maxNumberOfRetries() {
        return _maxRetries;
    }

    public void setMaxNumberOfRetries(int maxRetries) {
        _maxRetries = maxRetries;
    }

    /**
     * Retry interval in milliseconds.
     * 
     * @return
     */
    public int retryInterval() {
        return _retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        _retryInterval = retryInterval;
    }

    protected String toUrl(String path) throws Throwable {
        return toUrl(baseUrl(), path);
    }

    protected String serverHost() throws Throwable {
        return serverHost(baseUrl());
    }

    public static String toUrl(String baseUrl, String path) throws Throwable {
        return PathUtils.join(baseUrl, URLUtils.encode(path));
    }

    public static String serverHost(String baseUrl) throws Throwable {
        return URI.create(baseUrl).getHost();
    }

}
