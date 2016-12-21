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
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.environment;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
public class VaultIntegrationTest {

    private final File tmpDir;

    final String encFileDir;

    final String keystoreUrl;

    public VaultIntegrationTest() throws IOException {
        tmpDir = File.createTempFile("vault-karaf-itest", "tmp");
        tmpDir.delete();
        tmpDir.mkdirs();

        final File tmpEncFileDir = new File(tmpDir, "file-enc-dir");

        tmpEncFileDir.mkdir();

        final Path keyStorePath = Paths.get(tmpDir.getAbsolutePath(), "vault.keystore");
        Files.copy(VaultIntegrationTest.class.getResourceAsStream("/vault.keystore"), keyStorePath);

        final Path encFileVaultDatPath = Paths.get(tmpEncFileDir.getAbsolutePath(), "VAULT.dat");
        Files.copy(VaultIntegrationTest.class.getResourceAsStream("/file-enc-dir/VAULT.dat"), encFileVaultDatPath);

        keystoreUrl = keyStorePath.toAbsolutePath().toString();
        encFileDir = encFileVaultDatPath.getParent().toAbsolutePath().toString();
    }

    @Configuration
    public Option[] config() {
        final MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                .type("zip").versionAsInProject();

        final String[] vaultEnvironment = {"KEYSTORE_URL=" + keystoreUrl, "KEYSTORE_PASSWORD=MASK-EdCIIJbZZAl",
                "KEYSTORE_ALIAS=vault", "ENC_FILE_DIR=" + encFileDir, "SALT=Mxyzptlk", "ITERATION_COUNT=50"};

        return options(

                karafDistributionConfiguration().frameworkUrl(karafUrl),

                systemTimeout(10000),

                environment(vaultEnvironment),

                features(VaultIntegrationTest.class.getResource("/feature.xml").toString(), "vault-karaf-core"),

                vmOption("-Dprop=VAULT::block1::key::1"),

                mavenBundle("org.jboss.fuse.vault", "vault-karaf-core").versionAsInProject().start(),

                wrappedBundle(mavenBundle("org.assertj", "assertj-core").versionAsInProject())
                        .bundleSymbolicName("assertj"),

                logLevel(LogLevelOption.LogLevel.INFO));
    }

    @After
    public void deleteTemporaryDirectory() throws IOException {
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
