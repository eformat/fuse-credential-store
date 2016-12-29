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

import java.io.File;

import javax.inject.Inject;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PaxExam.class)
public class CreateVaultIntegrationTest extends BaseKarafTest {

    private Session session;

    @Inject
    protected SessionFactory sessionFactory;

    @Configuration
    public Option[] configuration() throws Exception {
        return withDefault();
    }

    @Before
    public void createSession() throws Exception {
        session = sessionFactory.create(System.in, System.out, System.err);
    }

    @Test
    public void shouldBeAbleToCreateVaultFromCommand() throws Exception {
        session.execute("vault:create -i 50 -p password -s '01234567' -v vault");

        assertThat(new File("vault/vault.keystore")).exists().isFile();
        assertThat(new File("vault/VAULT.dat")).exists().isFile();
    }
}
