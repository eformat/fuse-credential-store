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
package org.jboss.fuse.credential.store.karaf;

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

public abstract class BaseWithCredentialStoreSetupTest extends BaseKarafTest {

    private static final File TEST_CREDENTIAL_STORE_DIRECTORY;

    static {
        final String testCredentialStoreDirFromProperty = System.getProperty("test-dir");

        if (testCredentialStoreDirFromProperty != null) {
            TEST_CREDENTIAL_STORE_DIRECTORY = new File(testCredentialStoreDirFromProperty);
        } else {
            try {
                TEST_CREDENTIAL_STORE_DIRECTORY = File.createTempFile("test", "credential-store");
            } catch (final IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    @Inject
    private BundleContext bundleContext;

    static void deleteTemporaryDirectory() throws IOException {
        if (!TEST_CREDENTIAL_STORE_DIRECTORY.exists()) {
            return;
        }

        Files.walkFileTree(TEST_CREDENTIAL_STORE_DIRECTORY.toPath(), new SimpleFileVisitor<Path>() {
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

        TEST_CREDENTIAL_STORE_DIRECTORY.delete();
    }

    @After
    public void deleteTemporaryCredentialStoreDirectory() throws IOException {
        deleteTemporaryDirectory();
    }

    @Before
    public void resetCredentialStore() throws IOException, BundleException {
        final Bundle fuseCredentialStoreBundle = findBundle(
                "org.jboss.fuse.credential.store.fuse-credential-store-karaf");

        fuseCredentialStoreBundle.stop();

        setupCredentialStore();

        fuseCredentialStoreBundle.start();
    }

    public Option[] withSystemProperties(final Option... additional) throws Exception {
        setupCredentialStore();

        final String[] environment = {

                "CREDENTIAL_STORE_PROTECTION_ALGORITHM=masked-MD5-DES",

                "CREDENTIAL_STORE_PROTECTION_PARAMS=MDkEKXNvbWVhcmJpdHJhcnljcmF6eXN0cmluZ3RoYXRkb2Vzbm90bWF0dGVyAgID6AQIQt//5Ifg0x8=",

                "CREDENTIAL_STORE_PROTECTION=9KjAtKnaEnb3hgj+67wrS85IHABrZXBgG2gShcQ9kEGl4zjV9TLfyEwxBJ6836dI",

                "CREDENTIAL_STORE_ATTR_location=" + keyStorePath()

        };

        final Option[] options = options(environment(environment),

                features(IntegrationTest.class.getResource("/feature.xml").toString(), "fuse-credential-store-karaf"),

                vmOptions("-Dprop=CS:key", "-Dtest-dir=" + TEST_CREDENTIAL_STORE_DIRECTORY.getAbsolutePath())

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
        return Paths.get(TEST_CREDENTIAL_STORE_DIRECTORY.getAbsolutePath(), "credential.store");
    }

    void setupCredentialStore() throws IOException {
        deleteTemporaryDirectory();
        TEST_CREDENTIAL_STORE_DIRECTORY.mkdirs();

        Files.copy(IntegrationTest.class.getResourceAsStream("/credential.store"), keyStorePath());
    }
}
