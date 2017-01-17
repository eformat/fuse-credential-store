/**
 *  Copyright 2016-2017 Red Hat, Inc.
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
package org.jboss.fuse.credential.store.karaf;

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

public class IntegrationTest extends BaseWithCredentialStoreSetupTest {

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
    public void shouldBeAbleToCreateCredentialStoreFromCommand() throws Exception {
        session.execute(
                "credential-store:create -a location=newCredential.store -k password=\"be very, very quiet, I'm hunting rabbits\" -k algorithm=masked-MD5-DES");

        assertThat(new File("newCredential.store")).exists().isFile();
    }

    @Test
    public void shouldListCredentialStoreContentFromCommand() throws Exception {
        session.execute("credential-store:list");

        assertThat(new String(output.toByteArray())).contains("key", "CS:key");
    }

    @Test
    public void shouldProvideSystemProperties() {
        assertThat(System.getProperty("prop")).isEqualTo("this is a password");
    }

    @Test
    public void shouldRemoveFromCredentialStoreFromCommand() throws Exception {
        session.execute("credential-store:list");

        assertThat(new String(output.toByteArray())).contains("key", "CS:key");

        output.reset();

        session.execute("credential-store:remove -a key");

        session.execute("credential-store:list");

        assertThat(new String(output.toByteArray())).doesNotContain("key | CS:key");
    }

    @Test
    public void shouldStoreInCredentialStoreFromCommand() throws Exception {
        session.execute("credential-store:list");

        assertThat(new String(output.toByteArray())).contains("key", "CS:key");

        output.reset();

        session.execute("credential-store:store -a attribute2 -s secret");

        assertThat(new String(output.toByteArray()))
                .contains("Value stored in the credential store to reference it use: CS:attribute2");

        output.reset();

        session.execute("credential-store:list");

        assertThat(new String(output.toByteArray())).contains("attribute2", "CS:attribute2");
    }
}
