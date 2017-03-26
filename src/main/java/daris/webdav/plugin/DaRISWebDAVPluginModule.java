package daris.webdav.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import arc.mf.plugin.ConfigurationResolver;
import arc.mf.plugin.DataSinkRegistry;
import arc.mf.plugin.PluginModule;
import arc.mf.plugin.PluginService;
import daris.webdav.plugin.sink.OwnCloudSink;
import daris.webdav.plugin.sink.WebDAVSink;

public class DaRISWebDAVPluginModule implements PluginModule {

    private List<PluginService> _services;

    private WebDAVSink _webdavSink;
    private OwnCloudSink _owncloudSink;

    public DaRISWebDAVPluginModule() {
        _services = new ArrayList<PluginService>();
        // Add services here if any.
    }

    public String description() {
        return "Plugin sinks to access remove WebDAV server (or OwnCloud WebDAV server).";
    }

    public void initialize(ConfigurationResolver conf) throws Throwable {
        try {
            if (_webdavSink == null) {
                _webdavSink = new WebDAVSink();
                DataSinkRegistry.add(this, _webdavSink);
            }
            if (_owncloudSink == null) {
                _owncloudSink = new OwnCloudSink();
                DataSinkRegistry.add(this, _owncloudSink);
            }
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    public Collection<PluginService> services() {
        return _services;
    }

    public void shutdown(ConfigurationResolver conf) throws Throwable {
        try {
            DataSinkRegistry.removeAll(this);
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    public String vendor() {
        return "VicNode";
    }

    public String version() {
        return "1.0.0";
    }

}
