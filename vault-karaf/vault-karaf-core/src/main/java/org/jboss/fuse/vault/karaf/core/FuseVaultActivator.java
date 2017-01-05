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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;
import org.jboss.security.vault.SecurityVaultUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FuseVaultActivator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(FuseVaultActivator.class);

    private static final String SENSITIVE_VALUE_REPLACEMENT = "<sensitive>";

    private ServiceReference<MBeanServer> mbeanServerReference;

    private RuntimeMXBean original;

    private ObjectName runtimeBeanName;

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

        final Set<Object> replacedProperties = properties.entrySet().stream().filter(this::replace)
                .map(Map.Entry::getKey).collect(Collectors.toSet());

        if (!replacedProperties.isEmpty()) {
            installFilteringRuntimeBean(context, replacedProperties);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (original != null) {
            final MBeanServer mbeanServer = context.getService(mbeanServerReference);
            mbeanServer.unregisterMBean(runtimeBeanName);
            mbeanServer.registerMBean(original, runtimeBeanName);
        }
    }

    private Map<String, Object> environment() {
        @SuppressWarnings("unchecked")
        final Map<String, Object> env = (Map) System.getenv();

        return env;
    }

    private void installFilteringRuntimeBean(final BundleContext context, final Set<Object> replacedProperties)
            throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException,
            InstanceAlreadyExistsException, NotCompliantMBeanException {
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
                        for (final Object replacedProperty : replacedProperties) {
                            copy.put(replacedProperty, SENSITIVE_VALUE_REPLACEMENT);
                        }

                        return copy;
                    }

                    return result;
                });

        mbeanServer.unregisterMBean(runtimeBeanName);
        mbeanServer.registerMBean(proxy, runtimeBeanName);
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
