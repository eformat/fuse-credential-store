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
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.CommandException;
import org.jboss.fuse.vault.karaf.core.VaultHelper;
import org.jboss.security.plugins.PBEUtils;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultFactory;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;

@Command(scope = "vault", name = "create", description = "Create vault")
@Service
public final class CreateVault implements Action {

    private static final char[] INITIAL_PASSWORD = "somearbitrarycrazystringthatdoesnotmatter".toCharArray();

    private static final String KEYSTORE_FILENAME = "vault.keystore";

    private static final String PBE_ALGORITHM = "PBEwithMD5andDES";

    @Option(name = "-i", aliases = {"--iterations"},
            description = "Number of iterations to perform when masking the password", required = false,
            multiValued = false)
    private final int iterations = 100;

    @Option(name = "-p", aliases = {"--password"}, description = "Clear text password for the vault", required = true,
            multiValued = false)
    private String password;

    @Option(name = "-s", aliases = {"--salt"},
            description = "Salt used for masking the vault password, 8 printable ASCII characters", required = true,
            multiValued = false)
    private String salt;

    @Option(name = "-v", aliases = {"--vault"}, description = "Path to the directory that will contain the vault",
            required = true, multiValued = false)
    private String vaultPath;

    static String mask(final String passwd, final String salt, final int iterations) throws Exception {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);

        final PBEParameterSpec cipherSpec = new PBEParameterSpec(salt.getBytes(), iterations);

        final PBEKeySpec keySpec = new PBEKeySpec(INITIAL_PASSWORD);

        final SecretKey cipherKey = factory.generateSecret(keySpec);

        final String maskedPass = PBEUtils.encode64(passwd.getBytes(), PBE_ALGORITHM, cipherKey, cipherSpec);

        return PicketBoxSecurityVault.PASS_MASK_PREFIX + maskedPass;
    }

    public void createVaultIn(final File vaultDirectory, final String password, final String salt, final int iterations)
            throws Exception {
        final SecurityVault securityVault = SecurityVaultFactory.get();

        final Map<String, Object> options = new HashMap<>();
        final String keystoreUrl = new File(vaultDirectory, KEYSTORE_FILENAME).getAbsolutePath();
        options.put(PicketBoxSecurityVault.KEYSTORE_URL, keystoreUrl);

        options.put(PicketBoxSecurityVault.CREATE_KEYSTORE, "true");

        options.put(PicketBoxSecurityVault.SALT, salt);

        options.put(PicketBoxSecurityVault.ITERATION_COUNT, String.valueOf(iterations));

        final String maskedPassword = mask(password, salt, iterations);
        options.put(PicketBoxSecurityVault.KEYSTORE_PASSWORD, maskedPassword);

        options.put(PicketBoxSecurityVault.KEYSTORE_ALIAS, "vaultkey");

        final String encryptedDirectoryUrl = vaultDirectory.getAbsolutePath();
        options.put(PicketBoxSecurityVault.ENC_FILE_DIR, encryptedDirectoryUrl);

        securityVault.init(options);

        System.out.println("New vault was created in: " + vaultDirectory.getCanonicalPath());
        System.out.println("To use it specify the following environment variables:");
        System.out.println("export " + PicketBoxSecurityVault.KEYSTORE_URL + "=" + keystoreUrl);
        System.out.println("export " + PicketBoxSecurityVault.SALT + "=" + salt);
        System.out.println("export " + PicketBoxSecurityVault.ITERATION_COUNT + "=" + iterations);
        System.out.println("export " + PicketBoxSecurityVault.KEYSTORE_PASSWORD + "=" + maskedPassword);
        System.out.println("export " + PicketBoxSecurityVault.KEYSTORE_ALIAS + "=vaultkey");
        System.out.println("export " + PicketBoxSecurityVault.ENC_FILE_DIR + "=" + encryptedDirectoryUrl);

        options.remove(PicketBoxSecurityVault.CREATE_KEYSTORE);

        VaultHelper.initializeVault(options);
    }

    @Override
    public Object execute() throws Exception {
        final File vaultDirectory = new File(vaultPath);
        vaultDirectory.mkdirs();

        if (!vaultDirectory.exists() || !vaultDirectory.isDirectory()) {
            throw new CommandException("Path: " + vaultDirectory.getCanonicalPath()
                + " is not a directory. Check the path and file system permissions.");
        }

        if (!salt.matches("\\p{Print}{8}")) {
            throw new CommandException("Salt should be specified as 8 printable ASCII characters, given `" + salt
                + "`. Remember to put the salt value in quotes if it starts with 0 or contains whitespace characters");
        }

        createVaultIn(vaultDirectory, password, salt, iterations);

        return null;
    }

}
