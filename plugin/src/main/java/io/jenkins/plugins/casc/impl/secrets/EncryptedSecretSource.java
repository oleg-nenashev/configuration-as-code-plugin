package io.jenkins.plugins.casc.impl.secrets;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.util.Secret;
import io.jenkins.plugins.casc.SecretSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public class EncryptedSecretSource extends SecretSource {

    private static final Logger LOGGER = Logger.getLogger(EncryptedSecretDefinition.class.getName());
    private final String PRIVATE_KEY_PATH = System.getenv("JCASC_PRIVATE_KAY_PATH");
    static final String KEY_PATH_PROPERTY = EncryptedSecretSource.class.getName() + ".privateKeyPath";

    private static PrivateKey privateKey;

    @Override
    public Optional<String> reveal(String secret) {
        if (StringUtils.isBlank(secret) || !secret.startsWith("ENC")) {
            return Optional.empty();
        }

        try {
            final EncryptedSecretDefinition def = EncryptedSecretDefinition.parse(secret);
            return Optional.of(decrypt(def.getAlgorithm(), def.getEncryptedText()));
        } catch (IOException | GeneralSecurityException ex) {
            LOGGER.log(Level.WARNING, "Failed to parse the encrypted secret: " + secret, ex);
            return Optional.empty();
        }
    }

    private static PrivateKey readPrivateKey(String algorithm) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (privateKey != null) {
            return privateKey;
        }

        //TODO: Support environment
        String privateKeyPath = System.getProperty(KEY_PATH_PROPERTY);
        return readPrivateKey(privateKeyPath, algorithm);
    }

    private static byte[] readFileBytes(String filename) throws IOException {
        Path path = Paths.get(filename);
        return Files.readAllBytes(path);
    }

    private static PrivateKey readPrivateKey(String filename, String algorithm) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(readFileBytes(filename));
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePrivate(keySpec);
    }

    static String decrypt(String algorithm, String encryptedText) throws IOException, GeneralSecurityException {
        Cipher cipher = Secret.getCipher(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, readPrivateKey(algorithm));
        byte[] bytes = hudson.Util.fromHexString(encryptedText);
        int iterations = bytes.length / 256;
        if (bytes.length % 256 == 0) {
            iterations--;
        }
        for (int i = 0; i < iterations; ++i) {
            int offset = 256 * i;
            cipher.update(bytes, offset , offset + 255);
        }
        return new String(cipher.doFinal(bytes, iterations * 256, bytes.length-1));
    }

    @VisibleForTesting
    static String encrypt(String algorithm, String text) throws GeneralSecurityException, IOException {
        Cipher cipher = Secret.getCipher(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, readPrivateKey(algorithm));
        return hudson.Util.toHexString(cipher.doFinal(text.getBytes()));
    }

    private static class EncryptedSecretDefinition {
        private final String algorithm;
        private final String encryptedText;

        private EncryptedSecretDefinition(String algorithm, String encryptedText) {
            this.algorithm = algorithm;
            this.encryptedText = encryptedText;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getEncryptedText() {
            return encryptedText;
        }

        public static EncryptedSecretDefinition parse(@Nonnull String string) throws IOException {
            String[] tokens = string.split(",");
            if (tokens.length != 3) {
                throw new IOException("Wrong encrypted string");
            }
            return new EncryptedSecretDefinition(tokens[1], tokens[2]);
        }
    }
}
