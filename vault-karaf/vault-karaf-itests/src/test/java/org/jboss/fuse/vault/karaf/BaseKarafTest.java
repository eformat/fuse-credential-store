package org.jboss.fuse.vault.karaf;

import java.util.Arrays;
import java.util.stream.Stream;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

abstract class BaseKarafTest {

    public static Option[] withDefault(final Option... options) throws Exception {

        final MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
                .artifactId("apache-karaf-minimal").type("zip").versionAsInProject();

        final Option[] defaultOptions = options(

                karafDistributionConfiguration().frameworkUrl(karafUrl),

                systemTimeout(30000),

                features(CreateVaultIntegrationTest.class.getResource("/feature.xml").toString(), "vault-karaf-core"),

                mavenBundle("org.jboss.fuse.vault", "vault-karaf-core").versionAsInProject().start(),

                wrappedBundle(mavenBundle("org.assertj", "assertj-core").versionAsInProject())
                        .bundleSymbolicName("assertj"),

                logLevel(LogLevelOption.LogLevel.INFO)

        );

        return Stream.concat(Arrays.stream(defaultOptions), Arrays.stream(options)).toArray(Option[]::new);
    }

}
