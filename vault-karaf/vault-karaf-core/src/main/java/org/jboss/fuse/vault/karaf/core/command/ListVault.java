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
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.CommandException;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultFactory;
import org.picketbox.util.StringUtil;

import static org.jboss.fuse.vault.karaf.core.VaultHelper.formatReference;

@Command(scope = "vault", name = "list", description = "List the content of the vault")
@Service
public class ListVault implements Action {

    @Override
    public Object execute() throws Exception {
        final ShellTable table = new ShellTable();
        table.column(new Col("Block"));
        table.column(new Col("Attribute"));
        table.column(new Col("Reference"));

        final SecurityVault vault = SecurityVaultFactory.get();
        if (!vault.isInitialized()) {
            throw new CommandException("The vault is not initialized");
        }

        for (final String key : vault.keyList()) {
            final String[] blockAttribute = key.split(StringUtil.PROPERTY_DEFAULT_SEPARATOR);
            final String block = blockAttribute[0];
            final String attributeName = blockAttribute[1];

            table.addRow().addContent(block, attributeName, formatReference(block, attributeName));
        }

        table.print(System.out);

        return null;
    }

}
