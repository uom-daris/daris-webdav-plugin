package daris.webdav.client;

import daris.webdav.client.jackrabbit.JackRabbitOwnCloudClient;

public class OwnCloudClientFactory {

    public static OwnCloudClient getClient(String baseUrl, String username, String password) {
        return new JackRabbitOwnCloudClient(baseUrl, username, password);
    }

}
