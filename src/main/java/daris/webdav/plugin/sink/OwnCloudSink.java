package daris.webdav.plugin.sink;

import java.util.Map;

import arc.mf.plugin.dtype.LongType;
import daris.webdav.client.OwnCloudClient;
import daris.webdav.client.WebDAVClient;

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
    protected WebDAVClient getClient(Map<String, String> params) throws Throwable {
        OwnCloudClient client = new OwnCloudClient(params.get(PARAM_URL), null, params.get(PARAM_USERNAME),
                params.get(PARAM_PASSWORD));
        if (params.containsKey(PARAM_CHUNK_SIZE)) {
            long chunkSize = 0;
            String value = params.get(PARAM_CHUNK_SIZE);
            chunkSize = Long.parseLong(value);
            if (chunkSize > 0) {
                client.setChunkSize(chunkSize);
            }
        }
        return client;
    }

    public String description() throws Throwable {
        return "Owncloud WebDAV sink.";
    }
}
