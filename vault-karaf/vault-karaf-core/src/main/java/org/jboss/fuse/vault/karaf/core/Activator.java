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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.fuse.vault.karaf.core.VaultHelper.unloadVault;

public final class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private static final String SENSITIVE_VALUE_REPLACEMENT = "<sensitive>";

    private ServiceReference<MBeanServer> mbeanServerReference;

    private RuntimeMXBean original;

    private final Map<String, String> replacedProperties = new HashMap<>();

    private ObjectName runtimeBeanName;

    @Override
    public void start(final BundleContext context) throws Exception {
        final Properties properties = System.getProperties();

        @SuppressWarnings("unchecked")
        final Collection<String> values = (Collection) properties.values();

        final boolean hasValuesFromVault = values.stream().anyMatch(SecurityVaultUtil::isVaultFormat);

        if (!hasValuesFromVault && !VaultHelper.environmentContainsVaultConfiguration()) {
            return;
        }

        try {
            VaultHelper.initializeVaultFromEnvironment();
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

        @SuppressWarnings("unchecked")
        final Hashtable<String, String> propertiesAsStringEntries = (Hashtable) properties;

        for (final Entry<String, String> property : propertiesAsStringEntries.entrySet()) {
            final String key = property.getKey();
            final String value = property.getValue();

            if (replaced(key, value)) {
                replacedProperties.put(key, value);
            }
        }

        if (!replacedProperties.isEmpty()) {
            installFilteringRuntimeBean(context);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (original != null) {
            final MBeanServer mbeanServer = context.getService(mbeanServerReference);

            if (mbeanServer != null) {
                mbeanServer.unregisterMBean(runtimeBeanName);
                mbeanServer.registerMBean(original, runtimeBeanName);
            }
        }

        unloadVault();

        if (!replacedProperties.isEmpty()) {
            // restore original value references
            replacedProperties.forEach((k, v) -> System.setProperty(k, v));
            replacedProperties.clear();
        }
    }

    private void installFilteringRuntimeBean(final BundleContext context) throws JMException {
        mbeanServerReference = context.getServiceReference(MBeanServer.class);
        final MBeanServer mbeanServer = context.getService(mbeanServerReference);

        runtimeBeanName = ObjectName.getInstance("java.lang", "type", "Runtime");

        original = ManagementFactory.getRuntimeMXBean();

        final Object proxy = Proxy.newProxyInstance(runtimeBeanName.getClass().getClassLoader(),
                new Class[] {RuntimeMXBean.class}, (proxy1, method, args) -> {
                    final Object result = method.invoke(original, args);

                    if ("getSystemProperties".equals(method.getName())) {
                        @SuppressWarnings("unchecked")
                        final Map<Object, Object> originalValues = (Map) result;

                        final Map<Object, Object> copy = new HashMap<>(originalValues);
                        for (final String replacedProperty : replacedProperties.keySet()) {
                            copy.put(replacedProperty, SENSITIVE_VALUE_REPLACEMENT);
                        }

                        return copy;
                    }

                    return result;
                });

        mbeanServer.unregisterMBean(runtimeBeanName);
        mbeanServer.registerMBean(proxy, runtimeBeanName);
    }

    boolean replaced(final String key, final String value) {
        boolean replaced = false;

        if (SecurityVaultUtil.isVaultFormat(value)) {
            final SecurityVault securityVault = VaultHelper.currentVault().get();

            try {
                final String[] parts = value.split("::");

                final String block = parts[1];

                final String attribute = parts[2];

                final char[] clear = securityVault.retrieve(block, attribute, null);

                System.setProperty(key, String.valueOf(clear));
                replaced = true;
            } catch (final SecurityVaultException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return replaced;
    }
}
