/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick.
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

package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.util.Secret;
import java.util.Collections;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint;
import org.jenkinsci.plugins.docker.commons.tools.DockerTool;
import org.jenkinsci.plugins.docker.commons.util.SampleDockerBuilder;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ConfigTest {

    @Test
    void configRoundTrip(JenkinsRule r) throws Exception {
        CredentialsStore store =
                CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        IdCredentials serverCredentials = new DockerServerCredentials(
                CredentialsScope.GLOBAL,
                "serverCreds",
                null,
                Secret.fromString("clientKey"),
                "clientCertificate",
                "serverCaCertificate");
        store.addCredentials(Domain.global(), serverCredentials);
        IdCredentials registryCredentials =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "registryCreds", null, "me", "pass");
        store.addCredentials(Domain.global(), registryCredentials);
        SampleDockerBuilder b1 = new SampleDockerBuilder(
                new DockerServerEndpoint("", ""),
                new DockerRegistryEndpoint("http://dhe.mycorp.com/", registryCredentials.getId()));
        r.assertEqualDataBoundBeans(b1, r.configRoundtrip(b1));
        b1 = new SampleDockerBuilder(
                new DockerServerEndpoint("tcp://192.168.1.104:8333", serverCredentials.getId()),
                new DockerRegistryEndpoint("", ""));
        r.assertEqualDataBoundBeans(b1, r.configRoundtrip(b1));
        r.jenkins
                .getDescriptorByType(DockerTool.DescriptorImpl.class)
                .setInstallations(new DockerTool("Docker 1.5", "/usr/local/docker15", Collections.emptyList()));
        b1.setToolName("Docker 1.5");
        r.assertEqualDataBoundBeans(b1, r.configRoundtrip(b1));
    }
}
