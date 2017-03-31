package daris.webdav.client.sardine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.SardineImpl;

import daris.io.SizedInputStream;
import daris.util.PathUtils;
import daris.webdav.client.AbstractWebDAVClient;

public class SardineWebDAVClient extends AbstractWebDAVClient {

    public SardineWebDAVClient(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
    }

    @Override
    protected void put(String path, InputStream in, long length, String mimeType, Map<String, String> requestHeaders,
            int retry) throws Throwable {
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null) {
            mkcol(parentPath);
        }
        final String url = toUrl(path);
        List<Header> headers = new ArrayList<Header>();
        if (requestHeaders != null) {
            for (Map.Entry<String, String> h : requestHeaders.entrySet()) {
                headers.add(new BasicHeader(h.getKey(), h.getValue()));
            }
        }
        if (mimeType != null) {
            headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, mimeType));
        }
        headers.add(new BasicHeader(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE));
        SardineImpl sardine = new SardineImpl(username(), password());
        sardine.enablePreemptiveAuthentication(serverHost());
        SizedInputStream sin = new SizedInputStream(in, length);
        try {
            System.out.println(Thread.currentThread().getName() + ": put: " + url);
            sardine.put(url, new InputStreamEntity(sin, length), headers);
        } catch (SardineException e) {
            if (sin.bytesRead() == 0 || retry > 0) {
                Thread.sleep(retryInterval());
                System.out.println(Thread.currentThread().getName() + ": put retry left: " + retry);
                put(path, in, length, mimeType, requestHeaders, retry - 1);
            } else {
                throw e;
            }
        } finally {
            sardine.shutdown();
        }
    }

    @Override
    protected void mkcol(String path, int retry) throws Throwable {
        if (exists(path)) {
            return;
        }
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null) {
            mkcol(parentPath);
        }
        String url = toUrl(path);
        System.out.println(Thread.currentThread().getName() + ": mkcol: " + url);
        Sardine sardine = SardineFactory.begin(username(), password());
        try {
            sardine.createDirectory(url);
        } catch (com.github.sardine.impl.SardineException e) {
            if (retry > 0) {
                if (retryInterval() > 0) {
                    Thread.sleep(retryInterval());
                }
                System.out.println(Thread.currentThread().getName() + ": mkcol retry left: " + retry);
                mkcol(path, retry - 1);
            } else {
                throw e;
            }
        } finally {
            sardine.shutdown();
        }
    }

    @Override
    public boolean exists(String path) throws Throwable {
        String url = toUrl(baseUrl(), path);
        Sardine sardine = SardineFactory.begin(username(), password());
        try {
            return sardine.exists(url);
        } finally {
            sardine.shutdown();
        }
    }

}
