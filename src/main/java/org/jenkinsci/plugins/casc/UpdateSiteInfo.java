package org.jenkinsci.plugins.casc;

import hudson.model.UpdateSite;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a list of update sites based on a list of key,value pairs. The value is the url for the update center
 */
public class UpdateSiteInfo extends HashMap<String, UpdateSite> {

    public UpdateSiteInfo(Map<String,Map> config) {
        for(Map.Entry<String,Map> e : config.entrySet()) {
            put(e.getKey(), new UpdateSite(e.getKey(), (String)(e.getValue()).get("url")));
        }
    }
}
