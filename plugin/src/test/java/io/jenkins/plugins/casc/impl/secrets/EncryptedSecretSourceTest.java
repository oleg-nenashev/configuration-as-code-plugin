package io.jenkins.plugins.casc.impl.secrets;

import hudson.model.User;
import hudson.util.IOUtils;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.File;
import java.io.InputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.WithoutJenkins;

import static org.junit.Assert.assertEquals;

@For(EncryptedSecretSource.class)
public class EncryptedSecretSourceTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @ClassRule
    public static TemporaryFolder dir = new TemporaryFolder();

    public static final String testKey = "just for test";

    @BeforeClass
    public static void setUp() throws Exception {
        File keyFile = dir.newFile("encryptionKey");
        try (InputStream istream = EncryptedSecretSourceTest.class.getResourceAsStream("id_rsa.der")) {
            IOUtils.copy(istream, keyFile);
        }
        System.setProperty(EncryptedSecretSource.KEY_PATH_PROPERTY, keyFile.getAbsolutePath());
    }

    @AfterClass
    public static void tearDown() {
        System.clearProperty(EncryptedSecretSource.KEY_PATH_PROPERTY);
    }

    @Test
    @WithoutJenkins
    public void reencryptRoundtrip() throws Exception {
        String encrypted = EncryptedSecretSource.encrypt("RSA", "testPassword");
        String decrypted = EncryptedSecretSource.decrypt("RSA", encrypted);
        assertEquals("testPassword", decrypted);
    }

    @ConfiguredWithCode(value = "EncryptedSecretSource_1.yml", expected = IllegalArgumentException.class,
        message = "No hudson.security.AuthorizationStrategy implementation found for globalMatrix")
    @Test
    public void testThrowsSuggestion() throws Exception {
        User user = j.jenkins.getUser("admin");
        //assertEquals("testPassword", user.getProperty(U));
    }
}
