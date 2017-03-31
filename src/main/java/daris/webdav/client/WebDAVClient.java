package daris.webdav.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
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
import daris.util.URLUtils;

public class WebDAVClient {

    private String _serverUrl;
    private String _username;
    private String _password;
    private int _retry = 255;
    private int _retryInterval = 500;

    public WebDAVClient(String serverUrl, String username, String password) {
        _serverUrl = serverUrl;
        _username = username;
        _password = password;
    }

    public void put(String path, InputStream in, long length, String mimeType) throws Throwable {
        put(path, in, length, mimeType, null, _retry);
    }

    protected void put(String path, InputStream in, long length, String mimeType, Map<String, String> requestHeaders)
            throws Throwable {
        put(path, in, length, mimeType, requestHeaders, _retry);
    }

    protected void put(String path, InputStream in, long length, String mimeType, Map<String, String> requestHeaders,
            int retry) throws Throwable {
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null) {
            mkcol(parentPath, true);
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
        SardineImpl sardine = new SardineImpl(_username, _password);
        sardine.enablePreemptiveAuthentication(serverHost());
        SizedInputStream sin = new SizedInputStream(in, length);
        try {
            System.out.println(Thread.currentThread().getName() + ": put: " + url);
            sardine.put(url, new InputStreamEntity(sin, length), headers);
        } catch (SardineException e) {
            if (sin.bytesRead() == 0 || retry > 0) {
                Thread.sleep(_retryInterval);
                System.out.println(Thread.currentThread().getName() + ": put retry left: " + retry);
                put(path, in, length, mimeType, requestHeaders, retry - 1);
            } else {
                throw e;
            }
        } finally {
            sardine.shutdown();
        }
    }

    public void put(String path, File file) throws Throwable {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            put(path, in, file.length(), null);
        } finally {
            in.close();
        }
    }

    public void mkcol(String path, Boolean parents) throws Throwable {
        mkcol(path, parents, _retry);
    }

    protected void mkcol(String path, Boolean parents, int retry) throws Throwable {
        if (exists(path)) {
            return;
        }
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null && parents) {
            mkcol(parentPath, true);
        }
        String url = toUrl(path);
        System.out.println(Thread.currentThread().getName() + ": mkcol: " + url);
        Sardine sardine = SardineFactory.begin(_username, _password);
        try {
            sardine.createDirectory(url);
        } catch (com.github.sardine.impl.SardineException e) {
            if (retry > 0) {
                if (_retryInterval > 0) {
                    Thread.sleep(_retryInterval);
                }
                System.out.println(Thread.currentThread().getName() + ": mkcol retry left: " + retry);
                mkcol(path, parents, retry - 1);
            } else {
                throw e;
            }
        } finally {
            sardine.shutdown();
        }
    }

    public boolean exists(String path) throws Throwable {

        String url = toUrl(path);
        Sardine sardine = SardineFactory.begin(_username, _password);
        try {
            // _lock.lock();
            return sardine.exists(url);
        } finally {
            // _lock.unlock();
            sardine.shutdown();
        }
    }

    private String toUrl(String path) throws Throwable {
        return PathUtils.join(_serverUrl, URLUtils.encode(path));
    }

    private String serverHost() {
        return URI.create(_serverUrl).getHost();
    }

    public static void main(String[] args) throws Throwable {
    }

}
