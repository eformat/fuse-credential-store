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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jboss.security.vault.SecurityVault;

import static org.jboss.fuse.vault.karaf.core.VaultHelper.mandatoryVault;

@Command(scope = "vault", name = "remove", description = "Remove secret from the vault")
@Service
public class RemoveFromVault implements Action {

    private static final byte[] NOT_USED = null;

    @Option(name = "-a", aliases = {"--attribute"}, description = "Name of the attribute to remove", required = true,
            multiValued = false)
    private String attributeName;

    @Option(name = "-b", aliases = {"--vault-block"}, description = "Block in which the attribute is stored",
            required = true, multiValued = false)
    private String vaultBlock;

    @Override
    public Object execute() throws Exception {
        final SecurityVault vault = mandatoryVault();

        vault.remove(vaultBlock, attributeName, NOT_USED);

        return null;
    }

}
