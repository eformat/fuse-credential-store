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

import java.util.Map;
import java.util.Properties;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;
import org.jboss.security.vault.SecurityVaultUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class FuseVaultActivator implements BundleActivator {

    @Override
    public void start(final BundleContext context) throws Exception {
        final Map<String, Object> env = environment();

        initializeVault(env);

        final Properties properties = System.getProperties();

        properties.forEach(this::replace);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
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

    void replace(final Object key, final Object value) {
        replace((String) key, (String) value);
    }

    void replace(final String key, final String value) {
        if (SecurityVaultUtil.isVaultFormat(value)) {
            try {
                final char[] clear = SecurityVaultUtil.getValue(value);

                System.setProperty(key, String.valueOf(clear));
            } catch (final SecurityVaultException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
