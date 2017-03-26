package daris.webdav.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
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

public class WebDAVClient {

    private String _serverUrl;
    private String _username;
    private String _password;
    private HttpClient _client;

    public WebDAVClient(String host, int port, String scheme, String proxyHost, int proxyPort, String username,
            String password) {

        _serverUrl = scheme + "://" + host + ":" + port;
        _username = username;
        _password = password;
        HostConfiguration conf = new HostConfiguration();
        conf.setHost(host, port, scheme);
        if (proxyHost != null) {
            conf.setProxy(proxyHost, proxyPort);
        }
        _client = new HttpClient();
        _client.setHostConfiguration(conf);
        if (_username != null && _password != null) {
            Credentials creds = new UsernamePasswordCredentials(_username, _password);
            _client.getState().setCredentials(AuthScope.ANY, creds);
        }
    }

    public WebDAVClient(String serverUrl, String proxyAddress, String username, String password) throws Throwable {

        _serverUrl = serverUrl;
        _username = username;
        _password = password;
        URI serverUri = URI.create(serverUrl);
        HostConfiguration conf = new HostConfiguration();
        conf.setHost(serverUri.getHost(), serverUri.getPort(), serverUri.getScheme());
        if (proxyAddress != null) {
            String[] components = proxyAddress.split(":");
            String proxyHost = components[0];
            int proxyPort = Integer.parseInt(components[1]);
            conf.setProxy(proxyHost, proxyPort);
        }
        _client = new HttpClient();
        _client.setHostConfiguration(conf);
        if (username != null && password != null) {
            Credentials creds = new UsernamePasswordCredentials(username, password);
            _client.getState().setCredentials(AuthScope.ANY, creds);
        }
    }

    public WebDAVClient(String serverUrl, String username, String password) throws Throwable {
        this(serverUrl, null, username, password);
    }

    public void put(String path, InputStream in, long length, String mimeType) throws Throwable {
        put(path, in, length, mimeType, null);
    }

    protected void put(String path, InputStream in, long length, String mimeType, Map<String, String> requestHeaders)
            throws Throwable {
        String parentPath = PathUtils.getParent(path);
        if (parentPath != null) {
            mkcol(parentPath, true);
        }
        String url = PathUtils.join(_serverUrl, path);
        PutMethod method = new PutMethod(url);
        method.setRequestHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString((_username + ":" + _password).getBytes()));
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
        String url = PathUtils.join(_serverUrl, path);
        MkColMethod method = new MkColMethod(url);
        method.setRequestHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString((_username + ":" + _password).getBytes()));
        try {
            int code = _client.executeMethod(method);
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
        String url = PathUtils.join(_serverUrl, path);
        PropFindMethod method = new PropFindMethod(url, DavConstants.PROPFIND_ALL_PROP, 0);
        method.setRequestHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString((_username + ":" + _password).getBytes()));
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
