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
package org.jboss.fuse.vault.karaf.core.command;

import java.io.File;
import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateVaultTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void shouldCreateVaults() throws Exception {
        final File root = temp.getRoot();

        final File keystoreFile = new File(root, "vault.keystore");

        CreateVault.createVaultIn(root, "password", "saltsalt", 50);

        assertThat(keystoreFile).exists().isFile();
        assertThat(new File(root, "VAULT.dat")).exists().isFile();
    }

    @Test
    public void shouldMaskPasswords() throws Exception {
        final String password = "password123";
        final String salt = "Mxyzptlk";
        final int iterations = 50;

        final String masked = CreateVault.mask(password, salt, iterations);

        assertThat(masked).matches("MASK-.+");

        final PicketBoxSecurityVault vaultImpl = new PicketBoxSecurityVault();

        final Method method = PicketBoxSecurityVault.class.getDeclaredMethod("decode", String.class, String.class,
                int.class);
        method.setAccessible(true);

        final Object clear = method.invoke(vaultImpl, masked, new String(salt), iterations);

        assertThat(clear).isEqualTo(password);
    }
}
