package org.jboss.fuse.vault.karaf;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxGuardIntegrationTest extends BaseWithVaultSetupTest {

    @Configuration
    public Option[] configuration() throws Exception {
        return withSystemProperties();
    }

    @Test
    public void shouldNotAllowJmxAccessToUnauthenticatedPrincipals() throws Exception {
        final JMXServiceURL karafViaJmx = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root");

        final Map<String, Object> environment = new HashMap<>();
        final String[] credentials = {"karaf", "karaf"};
        environment.put(JMXConnector.CREDENTIALS, credentials);

        final JMXConnector connector = JMXConnectorFactory.connect(karafViaJmx, environment);

        final MBeanServerConnection connection = connector.getMBeanServerConnection();

        final RuntimeMXBean runtimeBean = ManagementFactory.newPlatformMXBeanProxy(connection, "java.lang:type=Runtime",
                RuntimeMXBean.class);

        final Map<String, String> systemProperties = runtimeBean.getSystemProperties();

        assertThat(systemProperties).containsEntry("prop", "<sensitive>");
    }
}
