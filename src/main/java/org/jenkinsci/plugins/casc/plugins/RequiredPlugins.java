package org.jenkinsci.plugins.casc.plugins;

import hudson.util.VersionNumber;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mads on 1/30/18.
 */
public class RequiredPlugins extends HashMap<String,VersionNumber> {
    public RequiredPlugins(Map<String,Map> requiredPlugins) {
        if(requiredPlugins != null) {
            for (Map.Entry<String, Map> e : requiredPlugins.entrySet()) {
                put(e.getKey(), new VersionNumber((String) e.getValue().get("version")));
            }
        }
    }
}
