package daris.webdav.client;

import java.io.InputStream;
import java.util.Random;

import daris.io.SizedInputStream;
import daris.util.CollectionUtils;

public class OwnCloudClient extends WebDAVClient {

    public static final String OC_CHUNKED_HEADER = "OC-Chunked";

    private long _chunkSize = 0;

    public OwnCloudClient(String serverUrl, String username, String password) {
        super(serverUrl, username, password);
    }

    public long chunkSize() {
        return _chunkSize;
    }

    public void setChunkSize(long chunkSize) {
        _chunkSize = chunkSize;
    }

    public void putChunk(String filePath, InputStream in, long length, int index, int nbChunks) throws Throwable {
        String chunkPath = filePath + "-chunking-" + Math.abs((new Random()).nextInt(9000) + 1000) + "-" + nbChunks
                + "-" + index;
        super.put(chunkPath, in, length, null, CollectionUtils.createMap(OC_CHUNKED_HEADER, OC_CHUNKED_HEADER));
    }

    public void put(String path, InputStream in, long length, String mimeType) throws Throwable {
        if (_chunkSize <= 0 || length < 0 || length < _chunkSize) {
            // fallback to unchunked put if:
            // ---- _chunkSize<=0: configured unchunked
            // ---- _length<0: unknown length
            super.put(path, in, length, mimeType);
        } else {
            // TODO multi-threading.
            int nbChunks = (int) Math.ceil((double) length / _chunkSize);
            long remaining = length;
            try {
                for (int chunkIndex = 0; chunkIndex < nbChunks; chunkIndex++, remaining -= _chunkSize) {
                    long chunkLength = remaining >= _chunkSize ? _chunkSize : remaining;
                    putChunk(path, new SizedInputStream(in, chunkLength, false), chunkLength, chunkIndex, nbChunks);
                }
            } finally {
                in.close();
            }
        }
    }
}
