package daris.webdav.plugin.services;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import arc.archive.ArchiveInput;
import arc.archive.ArchiveRegistry;
import arc.mf.plugin.PluginLog;
import arc.mf.plugin.PluginTask;
import arc.mf.plugin.PluginThread;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.mime.NamedMimeType;
import arc.streams.SizedInputStream;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import daris.plugin.sink.util.OutputPath;
import daris.util.PathUtils;
import daris.webdav.client.WebDAVClient;

public class SvcWebDAVPut extends AbstractWebDAVPluginService {

    public static final String SERVICE_NAME = "daris.webdav.put";

    public SvcWebDAVPut() {
        Interface.Element id = new Interface.Element("id", AssetType.DEFAULT,
                "The id of a source asset. If not specified, 'where' must be specified.", 0, Integer.MAX_VALUE);
        id.add(new Interface.Attribute("path", StringType.DEFAULT,
                "The destination path for the asset. If not specified, defaults to asset path in Mediaflux.", 0));
        defn.add(id);

        Interface.Element where = new Interface.Element("where", StringType.DEFAULT,
                "A query to find the source assets. If not specified, 'id' must be specified.", 0, 1);
        where.add(new Interface.Attribute("expr", StringType.DEFAULT,
                "The expression to generate destnination path for each asset. See asset.path.generate for details. If not given, defaults to asset path in Mediaflux.",
                0));
        defn.add(where);

        defn.add(new Interface.Element("directory", StringType.DEFAULT, "The destination directory path.", 0, 1));

        defn.add(
                new Interface.Element("unarchive", BooleanType.DEFAULT, "Extracts archives. Defaults to false.", 0, 1));

        defn.add(new Interface.Element("max-threads", IntegerType.POSITIVE_ONE,
                "Maximum number of threads to process the inputs. Defaults to 1.", 0, 1));
    }

    @Override
    protected void execute(WebDAVClient client, Element args, Inputs inputs, Outputs outputs, XmlWriter w)
            throws Throwable {
        if (!args.elementExists("id") && !args.elementExists("where")) {
            throw new IllegalArgumentException("'id' or 'where' must be specified.");
        }
        if (args.elementExists("id") && args.elementExists("where")) {
            throw new IllegalArgumentException("both 'id' and 'where' are specified.");
        }
        String baseDirPath = args.value("directory");
        boolean unarchive = args.booleanValue("unarchive");
        int maxThreads = args.intValue("max-threads", 1);

        BlockingQueue<SimpleEntry<String, String>> inputQueue = null;
        if (args.elementExists("where")) {
            String where = args.value("where");
            String expr = args.value("where/@expr");
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("where", where);
            dm.add("size", "infinity");
            dm.add("action", "pipe");
            if (maxThreads > 1) {
                dm.add("pipe-nb-threads", maxThreads);
            }
            dm.add("pipe-generate-result-xml", true);
            dm.push("service", new String[] { "name", "asset.path.generate" });
            dm.add("expr", expr);
            dm.pop();
            List<XmlDoc.Element> pes = executor().execute("asset.query", dm.root()).elements("path");
            if (pes != null && !pes.isEmpty()) {
                inputQueue = new ArrayBlockingQueue<SimpleEntry<String, String>>(pes.size(), false);
                for (XmlDoc.Element pe : pes) {
                    inputQueue.add(new SimpleEntry<String, String>(pe.value("@id"), pe.value()));
                }
            }
        } else {
            List<XmlDoc.Element> ies = args.elements("id");
            inputQueue = new ArrayBlockingQueue<SimpleEntry<String, String>>(ies.size(), false);
            for (XmlDoc.Element ie : ies) {
                inputQueue.add(new SimpleEntry<String, String>(ie.value(), ie.value("@path")));
            }
        }
        if (inputQueue == null || inputQueue.isEmpty()) {
            return;
        }
        int nbInputs = inputQueue.size();

        Processor[] processors = new Processor[maxThreads];
        for (int i = 0; i < maxThreads; i++) {
            processors[i] = new Processor(inputQueue, client, baseDirPath, unarchive);
            PluginThread.executeAsync("Worker thread of " + name(), processors[i]);
        }

        try {
            int lastProcessed = 0;
            while (true) {
                int nbTerminated = 0;
                int nbProcessed = 0;
                for (Processor c : processors) {
                    nbProcessed += c.processed();
                    if (c.isTerminated()) {
                        nbTerminated++;
                        Throwable error = c.error();
                        if (error != null) {
                            throw new Exception("Exception in worker thread: " + error.getMessage(), error);
                        }
                    }
                }
                PluginTask.threadTaskCompletedMultipleOf(nbProcessed - lastProcessed, nbInputs);
                lastProcessed = nbProcessed;
                if (nbTerminated == maxThreads) {
                    break;
                }
                PluginTask.checkIfThreadTaskAborted();
                Thread.sleep(500);
            }
            PluginTask.threadTaskCompletedMultipleOf(nbInputs, nbInputs);
            PluginTask.setCurrentThreadActivity("Completed.");
        } finally {
            for (Processor consumer : processors) {
                if (!consumer.isTerminated()) {
                    PluginLog.log().add(PluginLog.WARNING, "Terminating " + consumer.threadName() + "...");
                    consumer.interrupt();
                }
            }
        }
    }

    @Override
    public Access access() {
        return ACCESS_ACCESS;
    }

    @Override
    public String description() {
        return "Upload assets to the specified WebDAV server.";
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    private static class Processor implements Runnable {

        private BlockingQueue<SimpleEntry<String, String>> _inputs;
        private WebDAVClient _client;
        private String _baseDirPath;
        private boolean _unarchive;

        private Thread _thread;
        private ServiceExecutor _executor;
        private PluginLog _log;

        private boolean _terminated = false;
        private Throwable _error = null;
        private int _processed = 0;

        public Processor(BlockingQueue<SimpleEntry<String, String>> inputs, WebDAVClient client, String baseDirPath,
                boolean unarchive) {
            _inputs = inputs;
            _client = client;
            _baseDirPath = baseDirPath;
            _unarchive = unarchive;

            _terminated = false;
            _error = null;
            _processed = 0;
        }

        public String threadName() {
            if (_thread == null) {
                return null;
            }
            return _thread.getName();
        }

        public synchronized boolean isTerminated() {
            return _terminated;
        }

        private synchronized void setTerminated() {
            _terminated = true;
        }

        public synchronized Throwable error() {
            return _error;
        }

        private synchronized void setError(Throwable error) {
            _error = error;
        }

        public synchronized int processed() {
            return _processed;
        }

        private synchronized void incProcessed() {
            _processed++;
        }

        public synchronized void interrupt() {
            if (_thread != null) {
                _thread.interrupt();
            }
        }

        @Override
        public void run() {
            _thread = Thread.currentThread();
            _executor = PluginThread.serviceExecutor();
            _log = PluginLog.log();
            try {
                while (true) {
                    if (_thread.isInterrupted()) {
                        _log.add(PluginLog.WARNING, _thread.getName() + " has already been interrupted.");
                        break;
                    }
                    SimpleEntry<String, String> entry = _inputs.poll();
                    if (entry == null) {
                        _log.add(PluginLog.INFORMATION, _thread.getName() + " completed as input queue is empty.");
                        break;
                    }
                    String assetId = entry.getKey();
                    String path = entry.getValue();
                    putAsset(_executor, assetId, path, _client, _baseDirPath, _unarchive);
                    incProcessed();
                }
            } catch (Throwable t) {
                if (t instanceof InterruptedException) {
                    // aborted by main thread
                    _log.add(PluginLog.WARNING, _thread.getName() + " is interrupted.");
                } else {
                    setError(t);
                }
            } finally {
                setTerminated();
            }

        }

        private static void putAsset(ServiceExecutor executor, String assetId, String path, WebDAVClient client,
                String baseDirPath, boolean unarchive) throws Throwable {
            Outputs outputs = new Outputs(1);
            XmlDoc.Element ae = executor.execute("asset.get", "<args><id>" + assetId + "</id></args>", null, outputs)
                    .element("asset");
            Output output = outputs.output(0);

            String mimeType = output.mimeType();
            if (mimeType == null) {
                mimeType = ae.value("content/type");
            }
            if (!ArchiveRegistry.isAnArchive(mimeType) && unarchive) {
                unarchive = false;
            }
            try {
                if (unarchive) {
                    String destDirPath = OutputPath.getOutputPath(baseDirPath, path, null, ae, unarchive);
                    ArchiveInput ai = ArchiveRegistry.createInput(
                            new SizedInputStream(output.stream(), output.length()), new NamedMimeType(mimeType));
                    ArchiveInput.Entry entry;
                    try {
                        while ((entry = ai.next()) != null) {
                            if (entry.isDirectory()) {
                                client.mkcol(PathUtils.join(destDirPath, entry.name()));
                            } else {
                                try {
                                    client.put(PathUtils.join(destDirPath, entry.name()), entry.stream(), entry.size(),
                                            entry.mimeType());
                                } finally {
                                    entry.stream().close();
                                }
                            }
                        }
                    } finally {
                        ai.close();
                    }
                } else {
                    String destFilePath = OutputPath.getOutputPath(baseDirPath, path, null, ae, unarchive);
                    client.put(destFilePath, output.stream(), output.length(), mimeType);
                }
            } finally {
                output.stream().close();
                output.close();
            }
        }

    }

}
