package daris.webdav.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;

import daris.io.IOUtils;
import daris.util.PathUtils;
import daris.util.URLUtils;

public class WebDAVClient {

    private String _serverUrl;
    private HostConfiguration _hostConf;
    private UsernamePasswordCredentials _credentials;
    private String _authorization;
    private HttpClient _client;

    public WebDAVClient(String serverUrl, String proxyHost, int proxyPort, String username, String password) {
        _serverUrl = serverUrl;
        URI serverUri = URI.create(serverUrl);
        _hostConf = new HostConfiguration();
        _hostConf.setHost(serverUri.getHost(), serverUri.getPort(), serverUri.getScheme());
        if (proxyHost != null) {
            _hostConf.setProxy(proxyHost, proxyPort);
        }
        _credentials = new UsernamePasswordCredentials(username, password);
        _authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        _client = new HttpClient();
        _client.setHostConfiguration(_hostConf);
        _client.getState().setCredentials(AuthScope.ANY, _credentials);
    }

    public WebDAVClient(String serverUrl, String proxyAddress, String username, String password) throws Throwable {
        this(serverUrl, proxyAddress == null ? null : proxyAddress.split(":")[0],
                proxyAddress == null ? 0 : (Integer.parseInt(proxyAddress.split(":")[1])), username, password);
    }

    public WebDAVClient(String serverUrl, String username, String password) throws Throwable {
        this(serverUrl, null, 0, username, password);
    }

    public void put(String path, InputStream in, long length, String mimeType) throws Throwable {
        put(path, in, length, mimeType, null);
    }

    protected void put(String path, InputStream in, long length, String mimeType, Map<String, String> requestHeaders)
            throws Throwable {
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null) {
            System.out.println("##DEBUG## making dir: " + parentPath);
            mkcol(parentPath, true);
        }
        String url = PathUtils.join(_serverUrl, URLUtils.encode(path));
        System.out.println("##DEBUG## putting file: " + url);
        PutMethod method = new PutMethod(url);
        method.setRequestHeader("Authorization", _authorization);
        if (length >= 0) {
            method.setRequestHeader("Content-Length", Long.toString(length));
        }
        if (mimeType != null) {
            method.setRequestHeader("Content-Type", mimeType);
        }
        if (requestHeaders != null) {
            for (String header : requestHeaders.keySet()) {
                method.setRequestHeader(header, requestHeaders.get(header));
            }
        }
        Header[] hs = method.getRequestHeaders();
        for (Header h : hs) {
            System.out.println("##DEBUG##: " + h.getName() + ": " + h.getValue());
        }
        RequestEntity requestEntity = new InputStreamRequestEntity(in);
        method.setRequestEntity(requestEntity);
        try {
            int code = _client.executeMethod(method);
            if (!method.succeeded()) {
                throw new HttpException("Failed to execute PUT method. HTTP response: " + code);
            }
            if (!(code == HttpStatus.SC_CREATED || code == HttpStatus.SC_NO_CONTENT)) {
                throw new HttpException("Unexpected HTTP response: " + code);
            }
        } finally {
            method.releaseConnection();
            in.close();
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
        if (exists(path)) {
            return;
        }
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null && parents) {
            mkcol(parentPath, true);
        }
        String url = PathUtils.join(_serverUrl, URLUtils.encode(path));
        MkColMethod method = new MkColMethod(url);
        method.setRequestHeader("Authorization", _authorization);
        try {
            int code = _client.executeMethod(method);
            if (code == HttpStatus.SC_METHOD_NOT_ALLOWED || code == HttpStatus.SC_FORBIDDEN
                    || code == HttpStatus.SC_CONFLICT) {
                // ignore the above errors (in multithreaded concurrent env)
                return;
            }
            if (!method.succeeded()) {
                throw new HttpException(
                        "Failed to execute MKCOL method. HTTP response: " + code + " " + method.getStatusText());
            }
            if (code != HttpStatus.SC_CREATED) {
                throw new HttpException("Unexpected HTTP response: " + code + " " + method.getStatusText());
            }
            IOUtils.exhaustInputStream(method.getResponseBodyAsStream());
        } finally {
            method.releaseConnection();
        }
    }

    public boolean exists(String path) throws Throwable {
        String url = PathUtils.join(_serverUrl, URLUtils.encode(path));
        PropFindMethod method = new PropFindMethod(url, DavConstants.PROPFIND_ALL_PROP, 0);
        method.setRequestHeader("Authorization", _authorization);
        try {
            _client.executeMethod(method);
            return method.getStatusCode() == HttpStatus.SC_MULTI_STATUS;
        } finally {
            method.releaseConnection();
        }
    }

    public static void main(String[] args) throws Throwable {
    }

}
