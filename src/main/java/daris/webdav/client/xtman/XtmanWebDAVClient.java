package daris.webdav.client.xtman;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import daris.io.SizedInputStream;
import daris.util.PathUtils;
import daris.webdav.client.AbstractWebDAVClient;

public class XtmanWebDAVClient extends AbstractWebDAVClient implements HttpClient {

    private ReentrantLock _lock;

    public XtmanWebDAVClient(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
        _lock = new ReentrantLock();
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
            System.out.println(Thread.currentThread().getName() + ": MKCOL: " + path);
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
            if (nbRetries > 0) {
                if (retryInterval() > 0) {
                    Thread.sleep(retryInterval());
                }
                System.out.println(Thread.currentThread().getName() + ": MKCOL retries left: " + nbRetries);
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
            System.out.println(Thread.currentThread().getName() + ": PUT: " + path);
            execute(true, path, "PUT", headers, sin, length, contentType, false, new HttpResponseHandler<Void>() {

                @Override
                public Void handleResponse(int responseCode, String responseMessage, Map<String, List<String>> headers,
                        InputStream responseContentInputStream) throws Throwable {
                    if (responseCode == HttpStatus.SC_CREATED || responseCode == HttpStatus.SC_NO_CONTENT) {
                        // success
                        return null;
                    } else {
                        throw new HttpResponseException(responseCode, responseMessage);
                    }
                }
            });
        } catch (HttpResponseException e) {
            if (sin.bytesRead() == 0 || nbRetries > 0) {
                if (retryInterval() > 0) {
                    Thread.sleep(retryInterval());
                }
                System.out.println(Thread.currentThread().getName() + ": PUT retries left: " + nbRetries);
                put(path, in, length, contentType, headers, nbRetries - 1);
            } else {
                throw e;
            }
        }
    }

    public <T> T execute(boolean lock, String path, String requestMethod, Map<String, String> requestHeaders,
            InputStream requestContentInputStream, long requestContentLength, String requestContentType,
            boolean doInput, HttpResponseHandler<T> responseHandler) throws Throwable {
        String uri = toUrl(path);
        return execute(uri, requestMethod, requestHeaders, requestContentInputStream, requestContentLength,
                requestContentType, responseHandler, lock);
    }

    public <T> T execute(String requestUri, String requestMethod, Map<String, String> requestHeaders,
            InputStream requestContentInputStream, long requestContentLength, String requestContentType,
            HttpResponseHandler<T> responseHandler, boolean lock) throws Throwable {
        try {
            if (lock) {
                _lock.lock();
            }
            return HttpClientBase.execute(username(), password(), requestUri, requestMethod, requestHeaders,
                    requestContentInputStream, requestContentLength, requestContentType, responseHandler);
        } finally {
            if (lock) {
                _lock.unlock();
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