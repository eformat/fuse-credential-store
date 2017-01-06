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

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;
import org.jboss.security.vault.SecurityVaultUtil;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;
import org.picketbox.util.StringUtil;

public final class VaultHelper {

    private static final String SHARED_KEY = "1";

    private VaultHelper() {
        // utility class
    }

    public static boolean environmentContainsVaultConfiguration() {
        final Map<String, Object> environment = environment();

        return environment.containsKey(PicketBoxSecurityVault.KEYSTORE_URL)
            && environment.containsKey(PicketBoxSecurityVault.SALT)
            && environment.containsKey(PicketBoxSecurityVault.ITERATION_COUNT)
            && environment.containsKey(PicketBoxSecurityVault.KEYSTORE_PASSWORD)
            && environment.containsKey(PicketBoxSecurityVault.KEYSTORE_ALIAS)
            && environment.containsKey(PicketBoxSecurityVault.ENC_FILE_DIR);
    }

    public static String formatReference(final String block, final String attributeName) {
        return SecurityVaultUtil.VAULT_PREFIX + StringUtil.PROPERTY_DEFAULT_SEPARATOR + block
            + StringUtil.PROPERTY_DEFAULT_SEPARATOR + attributeName + StringUtil.PROPERTY_DEFAULT_SEPARATOR
            + SHARED_KEY;
    }

    public static void initializeVault(final Map<String, Object> env) throws SecurityVaultException {
        final SecurityVault securityVault = SecurityVaultFactory.get();

        securityVault.init(env);
    }

    public static void initializeVaultFromEnvironment() throws SecurityVaultException {
        initializeVault(environment());
    }

    private static Map<String, Object> environment() {
        @SuppressWarnings("unchecked")
        final Map<String, Object> env = (Map) System.getenv();

        return env;
    }
}
