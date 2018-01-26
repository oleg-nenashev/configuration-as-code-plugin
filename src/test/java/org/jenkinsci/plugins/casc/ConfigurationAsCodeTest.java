package org.jenkinsci.plugins.casc;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConfigurationAsCodeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void init_test_from_accepted_sources() throws Exception {
        File tmpConfigFile = tempFolder.newFile("jenkins_tmp.yaml");
        tempFolder.newFile("jenkins_tmp2.yaml");
        assertEquals(1, ConfigurationAsCode.getConfigurationInput(tmpConfigFile.getAbsolutePath()).size());
        assertEquals(2, ConfigurationAsCode.getConfigurationInput(tempFolder.getRoot().getAbsolutePath()).size());
    }

    @Test
    public void init_read_plugins_first() throws  Exception {
        tempFolder.newFile("Ajenkins1.yaml");
        tempFolder.newFile("jenkins2.yaml");
        tempFolder.newFile("Zplsugins.yaml");
        File withContent = tempFolder.newFile("plugins.yaml");
        FileUtils.writeStringToFile(withContent, "plugins:");
        List<InputStream> cfgs = ConfigurationAsCode.getConfigurationInput(tempFolder.getRoot().getAbsolutePath());
        assertEquals("We expect four elements",4, cfgs.size());
        FileInputStream fis = (FileInputStream)cfgs.get(0);
        FileInputStream withFis = new FileInputStream(withContent);
        assertEquals(withFis.available(), fis.available());
    }
}
