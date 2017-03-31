package daris.webdav.client;

import daris.webdav.client.sardine.SardineOwnCloudClient;

public class OwnCloudClientFactory {

    public static OwnCloudClient getClient(String baseUrl, String username, String password) {
        return new SardineOwnCloudClient(baseUrl, username, password);
    }

}
