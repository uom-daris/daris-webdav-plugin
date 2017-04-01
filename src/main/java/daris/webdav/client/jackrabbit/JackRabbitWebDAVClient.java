package daris.webdav.client.jackrabbit;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;

import com.github.sardine.impl.methods.HttpMkCol;

import daris.io.IOUtils;
import daris.io.SizedInputStream;
import daris.util.PathUtils;
import daris.webdav.client.AbstractWebDAVClient;

public class JackRabbitWebDAVClient extends AbstractWebDAVClient implements Closeable {

    private static PoolingHttpClientConnectionManager _cm;

    public static PoolingHttpClientConnectionManager getConnectionManager() {
        if (_cm == null) {
            _cm = new PoolingHttpClientConnectionManager();
            _cm.setDefaultMaxPerRoute(1);
            _cm.setMaxTotal(1);
        } else {
            _cm.closeExpiredConnections();
            _cm.closeIdleConnections(5, TimeUnit.MINUTES);
        }
        return _cm;
    }

    private CloseableHttpClient _client;

    public JackRabbitWebDAVClient(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
        _client = HttpClients.custom().setDefaultCredentialsProvider(getCredentialsProvider(username, password))
                .setConnectionManager(getConnectionManager()).build();
    }

    public boolean exists(String path) throws Throwable {
        String url = toUrl(path);
        System.out.println("Exists: " + url);
        HttpHead request = new HttpHead(url);
        HttpClientContext context = HttpClientContext.create();
        context.removeAttribute(HttpClientContext.REDIRECT_LOCATIONS);
        // @formatter:off
        // context.setCredentialsProvider(getCredentialsProvider(username(), password()));
        // context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, new AuthState());
        // @formatter:on
        return _client.execute(request, new ResponseHandler<Boolean>() {

            @Override
            public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
                    return true;
                }
                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    return false;
                }
                throw new HttpResponseException(statusCode,
                        "Unexpected HTTP response: " + statusCode + " " + statusLine.getReasonPhrase());
            }
        }, context);
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
        String url = toUrl(path);
        HttpMkCol request = new HttpMkCol(url);
        HttpClientContext context = HttpClientContext.create();
        // @formatter:off
        // context.setCredentialsProvider(getCredentialsProvider(username(), password()));
        // context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, new AuthState());
        // @formatter:on
        try {
            context.removeAttribute(HttpClientContext.REDIRECT_LOCATIONS);
            System.out.println(Thread.currentThread().getName() + ": MKCOL: " + url);
            _client.execute(request, new ResponseHandler<Void>() {
                @Override
                public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    int statusCode = statusLine.getStatusCode();
                    if (statusCode != HttpStatus.SC_CREATED) {
                        throw new HttpResponseException(statusCode,
                                "Unexpected HTTP response: " + statusCode + " " + statusLine.getReasonPhrase());
                    }
                    return null;
                }
            }, context);
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
        String url = toUrl(path);
        if (exists(path)) {
            return;
        }
        HttpClientContext context = HttpClientContext.create();
        // @formatter:off
        // context.setCredentialsProvider(getCredentialsProvider(username(), password()));
        // context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, new AuthState());
        // @formatter:on
        HttpPut put = new HttpPut(url);
        System.out.println("PUT: " + url);
        SizedInputStream sin = new SizedInputStream(in, length);
        put.setEntity(new InputStreamEntity(sin, length));
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                put.addHeader(new BasicHeader(h.getKey(), h.getValue()));
            }
        }
        if (contentType != null) {
            put.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
        }
        try {
            _client.execute(put, new ResponseHandler<Void>() {

                @Override
                public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    int statusCode = statusLine.getStatusCode();
                    if (!(statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT)) {
                        throw new HttpResponseException(statusCode,
                                "Unexpected HTTP response: " + statusCode + " " + statusLine.getReasonPhrase());
                    }
                    return null;
                }
            }, context);
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

    @Override
    public void close() throws IOException {
        if (_client != null) {
            _client.close();
        }
    }

    private static CredentialsProvider getCredentialsProvider(String username, String password) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        if (username != null) {
            provider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.BASIC),
                    new UsernamePasswordCredentials(username, password));
            provider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.DIGEST),
                    new UsernamePasswordCredentials(username, password));
            provider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.SPNEGO),
                    new UsernamePasswordCredentials(username, password));
            provider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.KERBEROS),
                    new UsernamePasswordCredentials(username, password));
        }
        return provider;
    }
}
