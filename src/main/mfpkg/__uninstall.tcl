# ============================================================================
# Uninstall Plugins
# ============================================================================
set plugin_label           [string toupper PACKAGE_$package]
set plugin_namespace       /mflux/plugins/daris-webdav-plugin
set plugin_zip             daris-webdav-plugin.zip
set plugin_jar             daris-webdav-plugin.jar
set module_class           daris.webdav.plugin.DaRISWebDAVPluginModule

if { [xvalue exists [plugin.module.exists :path ${plugin_namespace}/${plugin_jar} :class ${module_class}]] == "true" } {
    plugin.module.remove :path ${plugin_namespace}/${plugin_jar} :class ${module_class}
}

if { [xvalue exists [asset.namespace.exists :namespace ${plugin_namespace}]] == "true" } {
    asset.namespace.destroy :namespace "${plugin_namespace}"
}

system.service.reload

srefresh
