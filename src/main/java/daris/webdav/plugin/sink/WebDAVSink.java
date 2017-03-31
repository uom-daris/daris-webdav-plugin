package daris.webdav.plugin.sink;

import java.util.Map;

import arc.archive.ArchiveInput;
import arc.archive.ArchiveRegistry;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.PasswordType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.dtype.UrlType;
import arc.mime.NamedMimeType;
import arc.streams.LongInputStream;
import arc.xml.XmlDoc.Element;
import daris.plugin.sink.AbstractDataSink;
import daris.plugin.sink.util.OutputPath;
import daris.util.PathUtils;
import daris.webdav.client.WebDAVClient;

public class WebDAVSink extends AbstractDataSink {

    public static final String TYPE_NAME = "daris-webdav";

    public static final String PARAM_URL = "url";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_DIRECTORY = "directory";
    public static final String PARAM_UNARCHIVE = "unarchive";

    protected WebDAVSink(String typeName) throws Throwable {
        super(typeName);

        /*
         * init param definitions
         */
        addParameterDefinition(PARAM_URL, UrlType.DEFAULT, "WebDAV server URL.");
        addParameterDefinition(PARAM_USERNAME, StringType.DEFAULT, "WebDAV username.");
        addParameterDefinition(PARAM_PASSWORD, PasswordType.DEFAULT, "WebDAV password.");
        addParameterDefinition(PARAM_DIRECTORY, StringType.DEFAULT,
                "The default WebDAV directory(collection). If not specified, defaults to root /.");
        addParameterDefinition(PARAM_UNARCHIVE, BooleanType.DEFAULT, "Extract archive contents. Defaults to false.");
    }

    public WebDAVSink() throws Throwable {
        this(TYPE_NAME);
    }

    public String[] acceptedTypes() throws Throwable {
        return null;
    }

    public Object beginMultiple(Map<String, String> params) throws Throwable {
        validateParams(params);
        return getClient(params);
    }

    public int compressionLevelRequired() {
        // don't care
        return -1;
    }

    public void consume(Object multiTransferContext, String path, Map<String, String> params, Element userMeta,
            Element assetMeta, LongInputStream in, String appMimeType, String streamMimeType, long length)
            throws Throwable {
        if (multiTransferContext == null) {
            // if it is in multi transfer context, params were already validated
            // in beginMultiple() method.
            validateParams(params);
        }
        String directory = params.get(PARAM_DIRECTORY);
        String assetSpecificOutputPath = multiTransferContext != null ? null : getAssetSpecificOutput(params);
        boolean unarchive = false;
        if (params.containsKey(PARAM_UNARCHIVE)) {
            try {
                unarchive = Boolean.parseBoolean(params.get(PARAM_UNARCHIVE));
            } catch (Throwable e) {
                unarchive = false;
            }
        }
        String mimeType = streamMimeType;
        if (mimeType == null && assetMeta != null) {
            mimeType = assetMeta.value("content/type");
        }
        if (!ArchiveRegistry.isAnArchive(mimeType) && unarchive) {
            unarchive = false;
        }
        WebDAVClient client = getClient(multiTransferContext, params);
        if (unarchive) {
            String remoteDirPath = OutputPath.getOutputPath(directory, assetSpecificOutputPath, path, assetMeta, true);
            ArchiveInput ai = ArchiveRegistry.createInput(in, new NamedMimeType(mimeType));
            ArchiveInput.Entry entry;
            try {
                while ((entry = ai.next()) != null) {
                    if (entry.isDirectory()) {
                        client.mkcol(PathUtils.join(remoteDirPath, entry.name()), true);
                    } else {
                        client.put(PathUtils.join(remoteDirPath, entry.name()), entry.stream(), entry.size(),
                                entry.mimeType());
                    }
                }
            } finally {
                ai.close();
            }
        } else {
            String remoteFilePath = OutputPath.getOutputPath(directory, assetSpecificOutputPath, path, assetMeta,
                    false);
            client.put(remoteFilePath, in, length, mimeType);
        }
    }

    public String description() throws Throwable {
        return "WebDAV sink.";
    }

    public void endMultiple(Object multiTransferContext) throws Throwable {

    }

    public void shutdown() throws Throwable {

    }

    protected void validateParams(Map<String, String> params) {
        if (!params.containsKey(PARAM_URL)) {
            throw new IllegalArgumentException("Missing url argument.");
        }
        if (!params.containsKey(PARAM_USERNAME)) {
            throw new IllegalArgumentException("Missing username argument.");
        }
        if (!params.containsKey(PARAM_PASSWORD)) {
            throw new IllegalArgumentException("Missing password argument.");
        }
    }

    protected WebDAVClient getClient(Map<String, String> params) throws Throwable {
        return new WebDAVClient(params.get(PARAM_URL), params.get(PARAM_USERNAME), params.get(PARAM_PASSWORD));
    }

    private WebDAVClient getClient(Object multiTransferContext, Map<String, String> params) throws Throwable {
        if (multiTransferContext != null) {
            return (WebDAVClient) multiTransferContext;
        } else {
            return getClient(params);
        }
    }

}
