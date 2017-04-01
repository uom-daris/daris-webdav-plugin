package daris.webdav.client;

import daris.webdav.client.jackrabbit.JackRabbitWebDAVClient;

public class WebDAVClientFactory {

    public static WebDAVClient getClient(String baseUrl, String username, String password) {
        return new JackRabbitWebDAVClient(baseUrl, username, password);
    }

}
