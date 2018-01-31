package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.model.UpdateSite;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.plugins.RequiredPlugins;
import org.jenkinsci.plugins.casc.plugins.UpdateSiteInfo;

import java.util.*;

/**
 * Created by mads on 1/29/18.
 */
@Extension(ordinal = Double.MAX_VALUE)
public class PluginConfigurator implements RootElementConfigurator {

    @Override
    public PluginManager configure(Object config) throws Exception {
        Map<?,?> map = (Map) config;

        HashMap<String, UpdateSite> allUpdateSites = new HashMap<>();
        Configurator<UpdateSiteInfo> configUpdateInfo = Configurator.lookup(UpdateSiteInfo.class);

        //Reuse the default update center
        List<UpdateSite> sites = Jenkins.getInstance().getUpdateCenter().getSites();
        for(UpdateSite us : sites) {
            if(us.getId().equals("default")) {
                allUpdateSites.put(us.getId(), us);
            }
        }

        //Extra sites from configuration
        UpdateSiteInfo in = configUpdateInfo.configure(map.get("updateSites"));
        allUpdateSites.putAll(in);

        //Clear all
        sites.clear();

        //Add ones from configuration
        sites.addAll(allUpdateSites.values());
        Jenkins.getInstance().save();

        //Jenkins.getInstance().getPluginManager().doCheckUpdatesServer();

        //Do check to see if we have installed the required plugins
        StringBuilder missingPlugins = new StringBuilder();
        RequiredPlugins requiredPlugins = new RequiredPlugins((Map<String,Map>)map.get("required"));

        for(Map.Entry<String,VersionNumber> requiredPlugin : requiredPlugins.entrySet()) {
            PluginWrapper plugin = Jenkins.getInstance().getPluginManager().getPlugin(requiredPlugin.getKey());
            if(plugin == null) {
                missingPlugins.append("Missing plugin: "+requiredPlugin.getKey()+"\n");
            } else if (plugin.getVersionNumber().isOlderThan(requiredPlugin.getValue())) {
                missingPlugins.append(String.format("Required plugin %s(%s) is older than the required version: %s",
                        plugin.getShortName(),
                        plugin.getVersion(),
                        requiredPlugin.getValue()) + "\n");
            }
        }

        if(!StringUtils.isBlank(missingPlugins.toString())) {
            //TODO: What is the proper reaction?
            throw new RuntimeException("Missing plugins detected: \n" + missingPlugins.toString());
        }

        return Jenkins.getInstance().getPluginManager();
    }

    @Override
    public String getName() {
        return "plugins";
    }

    /**
     * A set of fake attributes for the PluginManager. When configuring plugins we need the PluginManager to find
     * installed plugins but to update UpdateSites we need to configure the UpdateCenter list on the Jenkins instance.
     * @return
     */
    @Override
    public Set<Attribute> describe() {
        Set<Attribute> attr =  new HashSet<Attribute>();
        attr.add(new Attribute("proxy", ProxyConfiguration.class));
        attr.add(new Attribute("updateSites", UpdateSiteInfo.class));
        attr.add(new Attribute("required", RequiredPlugins.class));
        return attr;
    }
}
