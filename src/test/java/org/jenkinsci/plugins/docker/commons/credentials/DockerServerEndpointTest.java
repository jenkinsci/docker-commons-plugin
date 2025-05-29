/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.docker.commons.credentials;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.FreeStyleProject;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import hudson.util.Secret;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * @author Stephen Connolly
 */
public class DockerServerEndpointTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void smokes() throws Exception {
        DumbSlave slave = j.createOnlineSlave();
        VirtualChannel channel = slave.getChannel();
        FreeStyleProject item = j.createFreeStyleProject();
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        assertThat(store, instanceOf(SystemCredentialsProvider.StoreImpl.class));
        Domain domain = new Domain("docker", "A domain for docker credentials",
                Collections.<DomainSpecification>singletonList(new DockerServerDomainSpecification()));
        DockerServerCredentials credentials = new DockerServerCredentials(CredentialsScope.GLOBAL, "foo", "desc", Secret.fromString("a"), "b", "c");
        store.addDomain(domain, credentials);
        DockerServerEndpoint endpoint = new DockerServerEndpoint("tcp://localhost:2736", credentials.getId());
        FilePath dotDocker = DockerServerEndpoint.dotDocker(channel);
        List<FilePath> dotDockerKids = dotDocker.list();
        int initialSize = dotDockerKids == null ? 0 : dotDockerKids.size();
        KeyMaterialFactory factory = endpoint.newKeyMaterialFactory(item, channel);
        KeyMaterial2 keyMaterial = factory.materialize2();
        FilePath path = null;
        try {
            assertThat(keyMaterial.env().get("DOCKER_HOST", "missing"), is("tcp://localhost:2736"));
            assertThat(keyMaterial.env().get("DOCKER_TLS_VERIFY", "missing"), is("1"));
            assertThat(keyMaterial.env().get("DOCKER_CERT_PATH", "missing"), not("missing"));
            path = new FilePath(channel, keyMaterial.env().get("DOCKER_CERT_PATH", "missing"));
            if (!Functions.isWindows()) {
                assertThat(path.mode() & 0777, is(0700));
            }
            assertThat(path.child("key.pem").readToString(), is("a"));
            assertThat(path.child("cert.pem").readToString(), is("b"));
            assertThat(path.child("ca.pem").readToString(), is("c"));
        } finally {
            keyMaterial.close(channel);
        }
        assertThat(path.child("key.pem").exists(), is(false));
        assertThat(path.child("cert.pem").exists(), is(false));
        assertThat(path.child("ca.pem").exists(), is(false));
        assertThat(dotDocker.list().size(), is(initialSize));
    }
    
    
}
