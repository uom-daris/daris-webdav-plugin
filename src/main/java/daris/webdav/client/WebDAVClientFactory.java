package daris.webdav.client;

import daris.webdav.client.sardine.SardineWebDAVClient;

public class WebDAVClientFactory {

    public static WebDAVClient getClient(String baseUrl, String username, String password) {
        return new SardineWebDAVClient(baseUrl, username, password);
    }

}
