package daris.webdav.client;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import daris.io.SizedInputStream;
import daris.util.PathUtils;

public class WebDAVClientImpl extends AbstractWebDAVClient {

    private Map<String, Integer> _uriCxns;

    WebDAVClientImpl(String baseUrl, String username, String password, int maxNumberOfRetries, int retryInterval,
            int maxNumberOfConnectionsPerUri) {
        super(baseUrl, username, password, maxNumberOfRetries, retryInterval, maxNumberOfConnectionsPerUri);
        _uriCxns = Collections.synchronizedMap(new HashMap<String, Integer>());
    }

    @Override
    public boolean exists(String path) throws Throwable {
        return execute(false, path, "HEAD", null, null, 0, null, false, new HttpResponseHandler<Boolean>() {

            @Override
            public Boolean handleResponse(int responseCode, String responseMessage, Map<String, List<String>> headers,
                    InputStream responseContentInputStream) throws Throwable {
                if (responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                    return true;
                }
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return false;
                }
                throw new HttpResponseException(responseCode, responseMessage);
            }
        });
    }

    @Override
    protected void mkcol(String path, int nbRetries) throws Throwable {
        if (exists(path)) {
            return;
        }
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null) {
            mkcol(parentPath);
        }
        try {
            // System.out.println(Thread.currentThread().getName() + ": MKCOL: "
            // + path);
            execute(true, path, "MKCOL", null, null, 0, null, false, new HttpResponseHandler<Void>() {

                @Override
                public Void handleResponse(int responseCode, String responseMessage, Map<String, List<String>> headers,
                        InputStream responseContentInputStream) throws Throwable {
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        // success
                        return null;
                    } else {
                        throw new HttpResponseException(responseCode, responseMessage);
                    }
                }
            });
        } catch (HttpResponseException e) {
            if (!e.isUnauthorized() && nbRetries > 0) {
                if (retryInterval() > 0) {
                    Thread.sleep(retryInterval());
                }
                // System.out.println(Thread.currentThread().getName() + ":
                // MKCOL retries left: " + nbRetries);
                mkcol(path, nbRetries - 1);
            } else {
                throw e;
            }
        }
    }

    @Override
    protected void put(String path, InputStream in, long length, String contentType, Map<String, String> headers,
            int nbRetries) throws Throwable {
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null) {
            mkcol(parentPath);
        }
        SizedInputStream sin = new SizedInputStream(in, length);
        try {
            // @formatter:off
            // System.out.println(Thread.currentThread().getName() + ": PUT: " + path);
            // @formatter:on
            execute(true, path, "PUT", headers, sin, length, contentType, false, new HttpResponseHandler<Void>() {

                @Override
                public Void handleResponse(int responseCode, String responseMessage, Map<String, List<String>> headers,
                        InputStream responseContentInputStream) throws Throwable {
                    if (responseCode == HttpURLConnection.HTTP_CREATED
                            || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                        // success
                        return null;
                    } else {
                        throw new HttpResponseException(responseCode, responseMessage);
                    }
                }
            });
        } catch (HttpResponseException e) {
            if (!e.isUnauthorized() && sin.bytesRead() == 0 && nbRetries > 0) {
                if (retryInterval() > 0) {
                    Thread.sleep(retryInterval());
                }
                // @formatter:off
                // System.out.println(Thread.currentThread().getName() + ": PUT retries left: " + nbRetries);
                // @formatter:on
                put(path, in, length, contentType, headers, nbRetries - 1);
            } else {
                throw e;
            }
        }
    }

    protected <T> T execute(boolean lock, String path, String requestMethod, Map<String, String> requestHeaders,
            InputStream requestContentInputStream, long requestContentLength, String requestContentType,
            boolean doInput, HttpResponseHandler<T> responseHandler) throws Throwable {
        String uri = toUri(path);
        return execute(uri, requestMethod, requestHeaders, requestContentInputStream, requestContentLength,
                requestContentType, responseHandler, lock);
    }

    protected <T> T execute(String requestUri, String requestMethod, Map<String, String> requestHeaders,
            InputStream requestContentInputStream, long requestContentLength, String requestContentType,
            HttpResponseHandler<T> responseHandler, boolean lock) throws Throwable {

        try {
            if (lock && maxNumberOfConnectionPerUri() > 0) {
                synchronized (_uriCxns) {
                    int nbCxns = _uriCxns.containsKey(requestUri) ? _uriCxns.get(requestUri) : 0;
                    if (nbCxns >= maxNumberOfConnectionPerUri()) {
                        _uriCxns.wait();
                    }
                    _uriCxns.put(requestUri, _uriCxns.containsKey(requestUri) ? (_uriCxns.get(requestUri) + 1) : 1);
                }
            }
            return HttpClientBase.execute(username(), password(), requestUri, requestMethod, requestHeaders,
                    requestContentInputStream, requestContentLength, requestContentType, responseHandler);
        } finally {
            if (lock && maxNumberOfConnectionPerUri() > 0) {
                synchronized (_uriCxns) {
                    if (_uriCxns.containsKey(requestUri)) {
                        int nbCxns = _uriCxns.get(requestUri);
                        if (nbCxns <= 1) {
                            _uriCxns.remove(requestUri);
                        } else {
                            _uriCxns.put(requestUri, nbCxns - 1);
                        }
                        _uriCxns.notifyAll();
                    }
                }
            }
        }
    }

    @Override
    public <T> T execute(String requestUri, String requestMethod, Map<String, String> requestHeaders,
            InputStream requestContentInputStream, long requestContentLength, String requestContentType,
            HttpResponseHandler<T> responseHandler) throws Throwable {
        return execute(requestUri, requestMethod, requestHeaders, requestContentInputStream, requestContentLength,
                requestContentType, responseHandler, false);
    }

}