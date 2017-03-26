# ============================================================================
# Install Plugins
# ============================================================================
set plugin_label           [string toupper PACKAGE_$package]
set plugin_namespace       /mflux/plugins/daris-webdav-plugin
set plugin_zip             daris-webdav-plugin.zip
set plugin_jar             daris-webdav-plugin.jar
set module_class           daris.webdav.plugin.DaRISWebDAVPluginModule

# extract transform-plugin.jar to /mflux/plugins
asset.import :url archive:${plugin_zip} \
        :namespace -create yes ${plugin_namespace} \
        :label -create yes ${plugin_label} :label PUBLISHED \
        :update true

# remove the plugin module if pre-exists. Otherwise, cannot register the sink type.
if { [xvalue exists [plugin.module.exists :path ${plugin_namespace}/${plugin_jar} :class ${module_class}]] == "true" } {
    puts "Removing existing plugin module: ${module_class}"
	plugin.module.remove :path ${plugin_namespace}/${plugin_jar} :class ${module_class}
}

# install the plugin module
plugin.module.add :path ${plugin_namespace}/${plugin_jar} :class ${module_class} \
    :lib lib/jackrabbit-webdav-2.13.1.jar \
    :lib lib/slf4j-api-1.6.6.jar \
    :lib lib/slf4j-nop-1.7.21.jar \
    :lib lib/commons-codec-1.2.jar \
    :lib lib/commons-httpclient-3.1.jar \
    :lib lib/jcl-over-slf4j-1.7.4.jar

# reload the services     
system.service.reload

# refresh the enclosing shell
srefresh

