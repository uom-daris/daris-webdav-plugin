package daris.webdav.plugin.sink;

import java.util.Map;

import arc.mf.plugin.dtype.LongType;
import daris.webdav.client.OwnCloudClient;
import daris.webdav.client.OwnCloudClientFactory;

/**
 * 
 * Owncloud WebDAV server supports chunked uploads.
 * 
 * https://github.com/owncloud/core/wiki/spec:-big-file-chunking
 * 
 * @author wliu5
 *
 */
public class OwnCloudSink extends WebDAVSink {

    public static final String TYPE_NAME = "daris-owncloud";

    public static final String PARAM_CHUNK_SIZE = "chunk-size";

    public OwnCloudSink() throws Throwable {
        super(TYPE_NAME);
        addParameterDefinition(PARAM_CHUNK_SIZE, LongType.POSITIVE,
                "Chunk size for chunked upload. Defaults to 0, which disables chunking.");
    }

    @Override
    protected OwnCloudClient getClient(Map<String, String> params) throws Throwable {
        String baseUrl = params.get(PARAM_URL);
        String username = params.get(PARAM_USERNAME);
        String password = params.get(PARAM_PASSWORD);
        long chunkSize = OwnCloudClient.DEFAULT_CHUNK_SIZE;
        if (params.containsKey(PARAM_CHUNK_SIZE)) {
            chunkSize = Long.parseLong(params.get(PARAM_CHUNK_SIZE));
            if (chunkSize <= 0) {
                chunkSize = OwnCloudClient.DEFAULT_CHUNK_SIZE;
            }
        }
        OwnCloudClient client = OwnCloudClientFactory.create(baseUrl, username, password, chunkSize);
        return client;
    }

    public String description() throws Throwable {
        return "Owncloud WebDAV sink.";
    }
}
