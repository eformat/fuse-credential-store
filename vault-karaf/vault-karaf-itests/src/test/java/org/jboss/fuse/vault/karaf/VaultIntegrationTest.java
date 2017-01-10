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
package org.jboss.fuse.vault.karaf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import javax.inject.Inject;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.assertj.core.api.Assertions.assertThat;

public class VaultIntegrationTest extends BaseWithVaultSetupTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private Session session;

    @Inject
    protected SessionFactory sessionFactory;

    @Configuration
    public Option[] configuration() throws Exception {
        return withSystemProperties();
    }

    @Before
    public void createSession() throws Exception {
        session = sessionFactory.create(System.in, new PrintStream(output, true), System.err);
    }

    @Before
    public void resetOutput() {
        output.reset();
    }

    @Test
    public void shouldBeAbleToCreateVaultFromCommand() throws Exception {
        session.execute("vault:create -i 50 -p password -s '01234567' -v vault2");

        assertThat(new File("vault2/vault.keystore")).exists().isFile();
        assertThat(new File("vault2/VAULT.dat")).exists().isFile();
    }

    @Test
    public void shouldListVaultContentFromCommand() throws Exception {
        session.execute("vault:list");

        assertThat(new String(output.toByteArray())).contains("block1", "key", "VAULT::block1::key::1");
    }

    @Test
    public void shouldProvideSystemProperties() {
        assertThat(System.getProperty("prop")).isEqualTo("this is a password");
    }

    @Test
    public void shouldRemoveFromVaultFromCommand() throws Exception {
        session.execute("vault:list");

        assertThat(new String(output.toByteArray())).contains("block1", "key");

        output.reset();

        session.execute("vault:remove -a key -b block1");

        session.execute("vault:list");

        assertThat(new String(output.toByteArray())).doesNotContain("block1 | key");
    }

    @Test
    public void shouldStoreInVaultFromCommand() throws Exception {
        session.execute("vault:list");

        assertThat(new String(output.toByteArray())).contains("block1", "key");

        output.reset();

        session.execute("vault:store -a attribute2 -b block2 -x secret");

        assertThat(new String(output.toByteArray()))
                .contains("Value stored in vault to reference it use: VAULT::block2::attribute2::1");

        output.reset();

        session.execute("vault:list");

        assertThat(new String(output.toByteArray())).contains("block2", "attribute2");
    }
}
