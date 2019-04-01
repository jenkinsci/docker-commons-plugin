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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Base64;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.util.HashMap;
import java.util.Map;
import jenkins.model.Jenkins;
import jenkins.security.QueueItemAuthenticatorConfiguration;
import org.acegisecurity.Authentication;
import org.apache.commons.io.Charsets;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockQueueItemAuthenticator;
import org.jvnet.hudson.test.WithoutJenkins;

/**
 * @author Carlos Sanchez <carlos@apache.org>
 */
public class DockerRegistryEndpointTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @WithoutJenkins
    public void testParse() throws Exception {
        assertRegistry("https://index.docker.io/v1/", "acme/test");
        assertRegistry("https://index.docker.io/v1/", "busybox");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/acme/test");
        assertRegistry("https://docker.acme.com", "docker.acme.com/acme/test");
        assertRegistry("https://docker.acme.com", "docker.acme.com/busybox");
    }

    @Test
    @WithoutJenkins
    public void testParseWithTags() throws Exception {
        assertRegistry("https://index.docker.io/v1/", "acme/test:tag");
        assertRegistry("https://index.docker.io/v1/", "busybox:tag");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/acme/test:tag");
        assertRegistry("https://docker.acme.com", "docker.acme.com/acme/test:tag");
        assertRegistry("https://docker.acme.com", "docker.acme.com/busybox:tag");
    }

    @Issue("JENKINS-39181")
    @Test
    @WithoutJenkins
    public void testParseFullyQualifiedImageName() throws Exception {
        assertEquals("private-repo:5000/test-image", new DockerRegistryEndpoint("http://private-repo:5000/", null).imageName("private-repo:5000/test-image"));
        assertEquals("private-repo:5000/test-image", new DockerRegistryEndpoint("http://private-repo:5000/", null).imageName("test-image"));
    }

    @Issue("JENKINS-39181")
    @Test(expected = IllegalArgumentException.class)
    @WithoutJenkins
    public void testParseNullImageName() throws Exception {
        new DockerRegistryEndpoint("http://private-repo:5000/", null).imageName(null);
    }

    @Issue("JENKINS-39181")
    @Test(expected = IllegalArgumentException.class)
    @WithoutJenkins
    public void testParseNullUrlAndImageName() throws Exception {
        new DockerRegistryEndpoint(null, null).imageName(null);
    }

    @Issue("JENKINS-48437")
    @Test
    public void testGetTokenForRun() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy()
                .grant(Jenkins.READ).everywhere().to("alice", "bob")
                .grant(Computer.BUILD).everywhere().to("alice", "bob")
                // Item.CONFIGURE implies Credentials.USE_ITEM, which is what CredentialsProvider.findCredentialById
                // uses when determining whether to include item-scope credentials in the search.
                .grant(Item.CONFIGURE).everywhere().to("alice");
        j.jenkins.setAuthorizationStrategy(auth);

        String globalCredentialsId = "global-creds";
        IdCredentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
                globalCredentialsId, "test-global-creds", "user", "password");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), credentials);

        FreeStyleProject p1 = j.createFreeStyleProject();
        FreeStyleProject p2 = j.createFreeStyleProject();

        Map<String, Authentication> jobsToAuths = new HashMap<>();
        jobsToAuths.put(p1.getName(), User.getById("alice", true).impersonate());
        jobsToAuths.put(p2.getName(), User.getById("bob", true).impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().replace(new MockQueueItemAuthenticator(jobsToAuths));

        try (ACLContext as = ACL.as(User.getById("alice", false))) {
            DockerRegistryToken token = new DockerRegistryEndpoint("https://index.docker.io/v1/", globalCredentialsId)
                    .getToken(j.buildAndAssertSuccess(p1));
            Assert.assertNotNull("Alice has Credentials.USE_ITEM and should be able to use the credential", token);
            Assert.assertEquals("user", token.getEmail());
            Assert.assertEquals(Base64.getEncoder().encodeToString("user:password".getBytes(Charsets.UTF_8)), token.getToken());
        }

        try (ACLContext as = ACL.as(User.getById("bob", false))) {
            DockerRegistryToken token = new DockerRegistryEndpoint("https://index.docker.io/v1/", globalCredentialsId)
                    .getToken(j.buildAndAssertSuccess(p2));
            Assert.assertNull("Bob does not have Credentials.USE_ITEM and should not be able to use the credential", token);
        }
    }

    private void assertRegistry(String url, String repo) throws IOException {
        assertEquals(url, DockerRegistryEndpoint.fromImageName(repo, null).getEffectiveUrl().toString());
    }

}
