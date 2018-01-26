package org.jenkinsci.plugins.casc;

import hudson.Extension;

import java.util.Map;

/**
 * Created by mads on 1/29/18.
 */
@Extension
public class UpdateSiteConfigurator extends BaseConfigurator<UpdateSiteInfo> {

    @Override
    public Class<UpdateSiteInfo> getTarget() {
        return UpdateSiteInfo.class;
    }


    @Override
    public UpdateSiteInfo configure(Object config) throws Exception {
        return new UpdateSiteInfo((Map)config);
    }
}
