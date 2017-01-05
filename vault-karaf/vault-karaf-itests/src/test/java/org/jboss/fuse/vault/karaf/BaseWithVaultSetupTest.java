package org.jboss.fuse.vault.karaf;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.After;
import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.environment;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

public abstract class BaseWithVaultSetupTest extends BaseKarafTest {

    public static Option[] withSystemProperties(final Option... additional) throws Exception {
        final File tmpDir = File.createTempFile("vault-karaf-itest", "tmp");
        tmpDir.delete();
        tmpDir.mkdirs();

        final File tmpEncFileDir = new File(tmpDir, "file-enc-dir");

        tmpEncFileDir.mkdir();

        final Path keyStorePath = Paths.get(tmpDir.getAbsolutePath(), "vault.keystore");
        Files.copy(VaultIntegrationTest.class.getResourceAsStream("/vault.keystore"), keyStorePath);

        final Path encFileVaultDatPath = Paths.get(tmpEncFileDir.getAbsolutePath(), "VAULT.dat");
        Files.copy(VaultIntegrationTest.class.getResourceAsStream("/file-enc-dir/VAULT.dat"), encFileVaultDatPath);

        final String keystoreUrl = keyStorePath.toAbsolutePath().toString();
        final String encFileDir = encFileVaultDatPath.getParent().toAbsolutePath().toString();

        final String[] vaultEnvironment = {

                "KEYSTORE_URL=" + keystoreUrl,

                "KEYSTORE_PASSWORD=MASK-EdCIIJbZZAl",

                "KEYSTORE_ALIAS=vault",

                "ENC_FILE_DIR=" + encFileDir,

                "SALT=Mxyzptlk",

                "ITERATION_COUNT=50"};

        final Option[] options = Stream
                .concat(Arrays.stream(additional), Arrays.stream(options(environment(vaultEnvironment),

                        features(VaultIntegrationTest.class.getResource("/feature.xml").toString(), "vault-karaf-core"),

                        vmOptions("-Dprop=VAULT::block1::key::1",
                                "-DVaultIntegrationTest.tmpDir=" + tmpDir.getAbsolutePath())

                ))).toArray(Option[]::new);

        return withDefault(options);
    }

    @After
    public void deleteTemporaryDirectory() throws IOException {
        final File tmpDir = new File(System.getProperty("VaultIntegrationTest.tmpDir"));

        Files.walkFileTree(tmpDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

        });

        tmpDir.delete();
    }
}
