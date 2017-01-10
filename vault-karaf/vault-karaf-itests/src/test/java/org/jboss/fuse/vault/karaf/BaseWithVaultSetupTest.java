/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
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

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import static org.ops4j.pax.exam.CoreOptions.environment;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

public abstract class BaseWithVaultSetupTest extends BaseKarafTest {

    private static final File TEST_VAULT_DIRECTORY;

    static {
        final String testVaultDirFromProperty = System.getProperty("test-vault-dir");

        if (testVaultDirFromProperty != null) {
            TEST_VAULT_DIRECTORY = new File(testVaultDirFromProperty);
        } else {
            try {
                TEST_VAULT_DIRECTORY = File.createTempFile("test", "vault");
            } catch (final IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    @Inject
    private BundleContext bundleContext;

    static void deleteTemporaryDirectory() throws IOException {
        if (!TEST_VAULT_DIRECTORY.exists()) {
            return;
        }

        Files.walkFileTree(TEST_VAULT_DIRECTORY.toPath(), new SimpleFileVisitor<Path>() {
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

        TEST_VAULT_DIRECTORY.delete();
    }

    @After
    public void deleteTemporaryVaultDirectory() throws IOException {
        deleteTemporaryDirectory();
    }

    @Before
    public void resetVault() throws IOException, BundleException {
        final Bundle fuseVaultBundle = findBundle("org.jboss.fuse.vault.vault-karaf-core");

        fuseVaultBundle.stop();

        setupVault();

        fuseVaultBundle.start();
    }

    public Option[] withSystemProperties(final Option... additional) throws Exception {
        setupVault();

        final String[] vaultEnvironment = {

                "KEYSTORE_URL=" + keyStorePath(),

                "KEYSTORE_PASSWORD=MASK-EdCIIJbZZAl",

                "KEYSTORE_ALIAS=vault",

                "ENC_FILE_DIR=" + TEST_VAULT_DIRECTORY.getCanonicalPath(),

                "SALT=Mxyzptlk",

                "ITERATION_COUNT=50"};

        final Option[] options = options(environment(vaultEnvironment),

                features(VaultIntegrationTest.class.getResource("/feature.xml").toString(), "vault-karaf-core"),

                vmOptions("-Dprop=VAULT::block1::key::1", "-Dtest-vault-dir=" + TEST_VAULT_DIRECTORY.getAbsolutePath())

        );

        return Stream.concat(Arrays.stream(withDefault(options)), Arrays.stream(additional)).toArray(Option[]::new);
    }

    private Bundle findBundle(final String symbolicName) {
        for (final Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }

        throw new IllegalStateException("Unable to find bundle: " + symbolicName);
    }

    private Path keyStorePath() {
        return Paths.get(TEST_VAULT_DIRECTORY.getAbsolutePath(), "vault.keystore");
    }

    void setupVault() throws IOException {
        deleteTemporaryDirectory();
        TEST_VAULT_DIRECTORY.mkdirs();

        Files.copy(VaultIntegrationTest.class.getResourceAsStream("/vault.keystore"), keyStorePath());

        final Path vaultPath = Paths.get(TEST_VAULT_DIRECTORY.getAbsolutePath(), "VAULT.dat");

        Files.copy(VaultIntegrationTest.class.getResourceAsStream("/VAULT.dat"), vaultPath);
    }
}
