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

import org.junit.After;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.environment;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

public class VaultIntegrationTest extends BaseKarafTest {

    @Configuration
    public Option[] configuration() throws Exception {
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

        return withDefault(options(environment(vaultEnvironment),

                features(VaultIntegrationTest.class.getResource("/feature.xml").toString(), "vault-karaf-core"),

                vmOptions("-Dprop=VAULT::block1::key::1", "-DVaultIntegrationTest.tmpDir=" + tmpDir.getAbsolutePath())

        ));
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

    @Test
    public void shouldProvideSystemProperties() {
        assertThat(System.getProperty("prop")).isEqualTo("this is a password");
    }

}
