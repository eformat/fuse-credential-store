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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class BaseKarafTest {

    private final TemporaryFolder unpack = new TemporaryFolder();

    protected int jmxPort;

    static int freePort() {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(true);
            final int port = serverSocket.getLocalPort();

            return port;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String freePortAsString() {
        return Integer.toString(freePort());
    }

    @After
    public void delete() {
        unpack.delete();
    }

    public Option[] withDefault(final Option... options) throws Exception {

        unpack.create();

        final MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
                .artifactId("apache-karaf-minimal").type("zip").versionAsInProject();

        final String jmxPort = freePortAsString();

        final Option[] defaultOptions = options(

                karafDistributionConfiguration().frameworkUrl(karafUrl).unpackDirectory(unpack.getRoot()),

                configureConsole().ignoreRemoteShell().ignoreLocalConsole(),

                editConfigurationFilePut("etc/custom.properties", "karaf.shutdown.port", "-1"),

                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", jmxPort),

                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", freePortAsString()),

                systemTimeout(60000),

                features(BaseKarafTest.class.getResource("/feature.xml").toString(), "fuse-credential-store-karaf"),

                mavenBundle("org.jboss.fuse.credential.store", "fuse-credential-store-karaf").versionAsInProject()
                        .start(),

                wrappedBundle(mavenBundle("org.assertj", "assertj-core").versionAsInProject())
                        .bundleSymbolicName("assertj"),

                vmOptions("-Dtest-jmx-port=" + jmxPort),

                logLevel(LogLevelOption.LogLevel.WARN)

        );

        return Stream.concat(Arrays.stream(defaultOptions), Arrays.stream(options)).toArray(Option[]::new);
    }

}
