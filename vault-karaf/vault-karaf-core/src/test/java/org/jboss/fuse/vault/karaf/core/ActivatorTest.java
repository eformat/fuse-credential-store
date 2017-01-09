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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.jboss.security.vault.SecurityVaultException;
import org.junit.Before;
import org.junit.Test;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;

import static org.assertj.core.api.Assertions.assertThat;

public class ActivatorTest {

    final Activator activator = new Activator();

    @Before
    public void initialize() throws SecurityVaultException, UnsupportedEncodingException {
        final String keyStoreUrl = URLDecoder
                .decode(Activator.class.getResource("/vault.keystore").getFile().toString(), "UTF-8");
        final String markerPath = URLDecoder
                .decode(Activator.class.getResource("/file-enc-dir/.marker").getFile().toString(), "UTF-8");
        final String fileEncDir = markerPath.substring(0, markerPath.length() - 7);

        final Map<String, Object> env = new HashMap<>();
        env.put(PicketBoxSecurityVault.KEYSTORE_URL, keyStoreUrl);
        env.put(PicketBoxSecurityVault.KEYSTORE_PASSWORD, "MASK-EdCIIJbZZAl");
        env.put(PicketBoxSecurityVault.KEYSTORE_ALIAS, "vault");
        env.put(PicketBoxSecurityVault.ENC_FILE_DIR, fileEncDir);
        env.put(PicketBoxSecurityVault.SALT, "Mxyzptlk");
        env.put(PicketBoxSecurityVault.ITERATION_COUNT, "50");

        VaultHelper.initializeVault(env);
    }

    @Test
    public void shouldNotReplaceSystemPropertiesNotInVaultFormat() {
        activator.replace("key", "value");
    }

    @Test
    public void shouldReplaceSystemPropertiesInVaultFormat() {
        activator.replace("key", "VAULT::block1::key::1");

        assertThat(System.getProperty("key")).isEqualTo("this is a password");
    }
}
