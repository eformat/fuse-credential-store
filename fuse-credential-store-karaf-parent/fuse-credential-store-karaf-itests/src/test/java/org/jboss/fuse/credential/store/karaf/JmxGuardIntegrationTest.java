/**
 *  Copyright 2016-2017 Red Hat, Inc.
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

public class JmxGuardIntegrationTest extends BaseWithCredentialStoreSetupTest {

    private final String jmxPort = System.getProperty("test-jmx-port");

    @Configuration
    public Option[] configuration() throws Exception {
        return withSystemProperties();
    }

    @Test
    public void shouldNotAllowJmxAccessToUnauthenticatedPrincipals() throws Exception {
        final String jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:" + jmxPort + "/karaf-root";

        final JMXServiceURL karafViaJmx = new JMXServiceURL(jmxUrl);

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
