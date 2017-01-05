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
package org.jboss.fuse.vault.karaf.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;
import org.jboss.security.vault.SecurityVaultUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FuseVaultActivator implements BundleActivator {

    private static final String JMX_ACL_JAVA_LANG_RUNTIME = "jmx.acl.java.lang.Runtime";

    private static final Logger LOG = LoggerFactory.getLogger(FuseVaultActivator.class);

    private static final String ROLE_SENSITIVE = "sensitive";

    @Override
    public void start(final BundleContext context) throws Exception {
        final Properties properties = System.getProperties();

        @SuppressWarnings("unchecked")
        final Collection<String> values = (Collection) properties.values();

        final boolean hasValuesFromVault = values.stream().anyMatch(SecurityVaultUtil::isVaultFormat);

        if (!hasValuesFromVault) {
            return;
        }

        final Map<String, Object> env = environment();

        try {
            initializeVault(env);
        } catch (final SecurityVaultException e) {
            final String message = e.getMessage();
            System.err.println("\r\nUnable to initialize vault, destroying container: " + message);
            LOG.error("Unable to initialize vault, destroying container: {}", message);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Logging exception stack trace", e);
            }

            final Bundle frameworkBundle = context.getBundle(0);
            frameworkBundle.stop();

            return;
        }

        final boolean anyReplaced = properties.entrySet().stream().anyMatch(this::replace);

        if (anyReplaced) {
            configureSensitiveRoleAccessControl(context);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
    }

    private void configureSensitiveRoleAccessControl(final BundleContext context) throws IOException {
        final ServiceReference<ConfigurationAdmin> reference = context.getServiceReference(ConfigurationAdmin.class);
        final ConfigurationAdmin configurationAdmin = context.getService(reference);

        final Configuration configuration = configurationAdmin.getConfiguration(JMX_ACL_JAVA_LANG_RUNTIME);

        final Hashtable<String, Object> settings = new Hashtable<>();
        settings.put(Constants.SERVICE_PID, JMX_ACL_JAVA_LANG_RUNTIME);
        settings.put("getSystemProperties", ROLE_SENSITIVE);
        settings.put("SystemProperties", ROLE_SENSITIVE);

        configuration.update(settings);
    }

    private Map<String, Object> environment() {
        @SuppressWarnings("unchecked")
        final Map<String, Object> env = (Map) System.getenv();

        return env;
    }

    void initializeVault(final Map<String, Object> env) throws SecurityVaultException {
        final SecurityVault securityVault = SecurityVaultFactory.get();

        securityVault.init(env);
    }

    boolean replace(final Entry<Object, Object> entry) {
        final String key = (String) entry.getKey();
        final String value = (String) entry.getValue();

        return replace(key, value);
    }

    void replace(final Object key, final Object value) {
        replace((String) key, (String) value);
    }

    boolean replace(final String key, final String value) {
        boolean replaced = false;

        if (SecurityVaultUtil.isVaultFormat(value)) {
            try {
                final char[] clear = SecurityVaultUtil.getValue(value);

                System.setProperty(key, String.valueOf(clear));
                replaced = true;
            } catch (final SecurityVaultException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return replaced;
    }
}
