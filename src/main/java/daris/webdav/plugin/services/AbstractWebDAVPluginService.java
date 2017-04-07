package daris.webdav.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.PasswordType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.dtype.UrlType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;
import daris.webdav.client.WebDAVClient;
import daris.webdav.client.WebDAVClientFactory;

public abstract class AbstractWebDAVPluginService extends PluginService {

    protected Interface defn;

    AbstractWebDAVPluginService() {
        defn = new Interface();
        defn.add(new Interface.Element("url", UrlType.DEFAULT, "The WebDAV server url.", 1, 1));
        defn.add(new Interface.Element("username", StringType.DEFAULT, "The WebDAV username.", 1, 1));
        defn.add(new Interface.Element("password", PasswordType.DEFAULT, "The WebDAV password.", 1, 1));
    }

    @Override
    public final Interface definition() {
        return defn;
    }

    @Override
    public final void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String url = args.value("url");
        String username = args.value("username");
        String password = args.value("password");
        WebDAVClient client = WebDAVClientFactory.create(url, username, password);
        execute(client, args, inputs, outputs, w);
    }

    protected abstract void execute(WebDAVClient client, Element args, Inputs inputs, Outputs outputs, XmlWriter w)
            throws Throwable;
}
