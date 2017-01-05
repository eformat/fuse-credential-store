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
import java.io.PrintStream;

import javax.inject.Inject;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveFromVaultIntegrationTest extends BaseWithVaultSetupTest {

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

    @Test
    public void shouldRemoveFromVaultFromCommand() throws Exception {
        session.execute("vault:list");

        assertThat(new String(output.toByteArray())).contains("block1", "key");

        output.reset();

        session.execute("vault:remove -a key -b block1");

        session.execute("vault:list");

        assertThat(new String(output.toByteArray())).doesNotContain("block1 | key");

    }
}
