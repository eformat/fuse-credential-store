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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.ExamReactor;
import org.ops4j.pax.exam.spi.StagedExamReactor;
import org.ops4j.pax.exam.spi.reactors.ReactorManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

public class StartupFailureIntegrationTest {

    public static class Dummy {
        @Configuration
        public Option[] config() throws IOException {
            final MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
                    .artifactId("apache-karaf-minimal").type("zip").versionAsInProject();

            return options(

                    karafDistributionConfiguration().frameworkUrl(karafUrl),

                    configureConsole().ignoreRemoteShell().ignoreLocalConsole(),

                    systemTimeout(60000),

                    vmOptions("-Dprop=VAULT::block1::key::1"),

                    mavenBundle("org.jboss.fuse.vault", "vault-karaf-core").versionAsInProject().start(),

                    logLevel(LogLevelOption.LogLevel.WARN));
        }

        @Test
        public void dummy() {
        }
    }

    private StagedExamReactor stagedReactor;
    private PrintStream stderr;

    @After
    public void destroyContainer() {
        if (stderr != null) {
            System.setErr(stderr);
        }

        if (stagedReactor == null) {
            return;
        }

        stagedReactor.afterClass();
        stagedReactor.afterSuite();
    }

    @Before
    public void setupContainer() throws Exception {
        final Dummy dummy = new Dummy();

        final ReactorManager manager = ReactorManager.getInstance();

        final ExamReactor reactor = manager.prepareReactor(Dummy.class, dummy);

        final TestProbeBuilder probe = manager.createProbeBuilder(this);

        final TestAddress address = probe.addTest(Dummy.class, "dummy", new Object[0]);

        manager.storeTestMethod(address, null);

        reactor.addProbe(probe);

        stagedReactor = manager.stageReactor();
    }

    @Test
    public void shouldShutdownContainerOnVaultStartupFailure() throws Exception {
        stderr = System.err;

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(out));

        try {
            stagedReactor.invoke(stagedReactor.getTargets().iterator().next());

            fail("Container did not fail starting");
        } catch (final RuntimeException expected) {
            final String systemErrors = new String(out.toByteArray());

            assertThat(systemErrors).contains("Unable to initialize vault, destroying container");
        }
    }
}
