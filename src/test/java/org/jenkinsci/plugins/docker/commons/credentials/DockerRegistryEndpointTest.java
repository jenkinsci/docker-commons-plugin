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

import static org.junit.jupiter.api.Assertions.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jenkins.model.Jenkins;
import jenkins.security.QueueItemAuthenticatorConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockQueueItemAuthenticator;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Carlos Sanchez <carlos@apache.org>
 */
@WithJenkins
class DockerRegistryEndpointTest {

    @Test
    @WithoutJenkins
    void testParse() throws Exception {
        assertRegistry("https://index.docker.io/v1/", "acme/test");
        assertRegistry("https://index.docker.io/v1/", "busybox");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/acme/test");
        assertRegistry("https://docker.acme.com", "docker.acme.com/acme/test");
        assertRegistry("https://docker.acme.com", "docker.acme.com/busybox");
        // Registry v2
        assertRegistry("https://docker.acme.com", "docker.acme.com/path/to/busybox");
        assertRegistry("https://localhost:8080", "localhost:8080/path/to/busybox");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/path/to/busybox");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/path/to/busybox");
    }

    @Test
    @WithoutJenkins
    void testParseWithTags() throws Exception {
        assertRegistry("https://index.docker.io/v1/", "acme/test:tag");
        assertRegistry("https://index.docker.io/v1/", "busybox:tag");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/acme/test:tag");
        assertRegistry("https://docker.acme.com", "docker.acme.com/acme/test:tag");
        assertRegistry("https://docker.acme.com", "docker.acme.com/busybox:tag");
        assertRegistry("https://docker.acme.com", "docker.acme.com/busybox@sha256:sha256");
        // Registry v2
        assertRegistry("https://docker.acme.com", "docker.acme.com/path/to/busybox:tag");
        assertRegistry("https://localhost:8080", "localhost:8080/path/to/busybox:tag");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/path/to/busybox:tag");
        assertRegistry("https://docker.acme.com:8080", "docker.acme.com:8080/path/to/busybox@sha256:sha256");
    }

    @Issue("JENKINS-39181")
    @Test
    @WithoutJenkins
    void testParseFullyQualifiedImageName() throws Exception {
        assertEquals(
                "private-repo:5000/test-image",
                new DockerRegistryEndpoint("http://private-repo:5000/", null)
                        .imageName("private-repo:5000/test-image"));
        assertEquals(
                "private-repo:5000/test-image",
                new DockerRegistryEndpoint("http://private-repo:5000/", null).imageName("test-image"));
        assertEquals(
                "private-repo:5000/test-image:dev",
                new DockerRegistryEndpoint("http://private-repo:5000/", null)
                        .imageName("private-repo:5000/test-image:dev"));
        assertEquals(
                "private-repo:5000/test-image:dev",
                new DockerRegistryEndpoint("http://private-repo:5000/", null).imageName("test-image:dev"));
    }

    @Issue("JENKINS-39181")
    @Test
    @WithoutJenkins
    void testParseNullImageName() {
        assertThrows(IllegalArgumentException.class, () -> new DockerRegistryEndpoint("http://private-repo:5000/", null)
                .imageName(null));
    }

    @Issue("JENKINS-39181")
    @Test
    @WithoutJenkins
    void testParseNullUrlAndImageName() {
        assertThrows(IllegalArgumentException.class, () -> new DockerRegistryEndpoint(null, null).imageName(null));
    }

    @Issue("JENKINS-48437")
    @Test
    void testGetTokenForRun(JenkinsRule j) throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("alice", "bob")
                .grant(Computer.BUILD)
                .everywhere()
                .to("alice", "bob")
                // Item.CONFIGURE implies Credentials.USE_ITEM, which is what CredentialsProvider.findCredentialById
                // uses when determining whether to include item-scope credentials in the search.
                .grant(Item.CONFIGURE)
                .everywhere()
                .to("alice");
        j.jenkins.setAuthorizationStrategy(auth);

        String globalCredentialsId = "global-creds";
        IdCredentials credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, globalCredentialsId, "test-global-creds", "user", "password");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), credentials);

        FreeStyleProject p1 = j.createFreeStyleProject();
        FreeStyleProject p2 = j.createFreeStyleProject();

        QueueItemAuthenticatorConfiguration.get()
                .getAuthenticators()
                .replace(new MockQueueItemAuthenticator()
                        .authenticate(
                                p1.getFullName(), User.getById("alice", true).impersonate2())
                        .authenticate(
                                p2.getFullName(), User.getById("bob", true).impersonate2()));

        FreeStyleBuild r1 = j.buildAndAssertSuccess(p1);
        try (ACLContext as = ACL.as(User.getById("alice", false))) {
            DockerRegistryToken token =
                    new DockerRegistryEndpoint("https://index.docker.io/v1/", globalCredentialsId).getToken(r1);
            assertNotNull(token, "Alice has Credentials.USE_ITEM and should be able to use the credential");
            assertEquals("user", token.getEmail());
            assertEquals(
                    Base64.getEncoder().encodeToString("user:password".getBytes(StandardCharsets.UTF_8)),
                    token.getToken());
        }

        FreeStyleBuild r2 = j.buildAndAssertSuccess(p2);
        try (ACLContext as = ACL.as(User.getById("bob", false))) {
            DockerRegistryToken token =
                    new DockerRegistryEndpoint("https://index.docker.io/v1/", globalCredentialsId).getToken(r2);
            assertNull(token, "Bob does not have Credentials.USE_ITEM and should not be able to use the credential");
        }
    }

    private static void assertRegistry(String url, String repo) throws IOException {
        assertEquals(
                url,
                DockerRegistryEndpoint.fromImageName(repo, null)
                        .getEffectiveUrl()
                        .toString());
    }
}
