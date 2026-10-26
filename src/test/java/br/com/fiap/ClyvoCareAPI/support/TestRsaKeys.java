package br.com.fiap.ClyvoCareAPI.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

public abstract class TestRsaKeys {
    private static final Path DIRECTORY = createKeys();

    @DynamicPropertySource
    static void configureKeys(DynamicPropertyRegistry registry) {
        registry.add("rsa.private-key", () -> DIRECTORY.resolve("private.pem").toUri().toString());
        registry.add("rsa.public-key", () -> DIRECTORY.resolve("public.pem").toUri().toString());
    }

    private static Path createKeys() {
        try {
            Path directory = Files.createTempDirectory("clyvocare-test-keys-");
            directory.toFile().deleteOnExit();
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            writePem(directory.resolve("private.pem"), "PRIVATE KEY", pair.getPrivate().getEncoded());
            writePem(directory.resolve("public.pem"), "PUBLIC KEY", pair.getPublic().getEncoded());
            return directory;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot generate test RSA keys", exception);
        }
    }

    private static void writePem(Path path, String type, byte[] key) throws Exception {
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key);
        Files.writeString(path, "-----BEGIN " + type + "-----\n" + encoded + "\n-----END " + type + "-----\n");
        path.toFile().deleteOnExit();
    }
}
