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
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

public class ConfigTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Test public void configRoundTrip() throws Exception {
        // TODO how to create DockerServerCredentials? There are no implementations.
        // (There are none of DockerRegistryCredentials either, but at least DockerRegistryEndpoint accepts UsernamePasswordCredentials.)
        IdCredentials registryCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "registryCreds", null, "me", "pass");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), registryCredentials);
        SampleDockerBuilder b1 = new SampleDockerBuilder(new DockerServerEndpoint("", ""), new DockerRegistryEndpoint("http://dhe.mycorp.com/", registryCredentials.getId()));
        r.assertEqualDataBoundBeans(b1, r.configRoundtrip(b1));
        b1 = new SampleDockerBuilder(new DockerServerEndpoint("tcp://192.168.1.104:8333", ""), new DockerRegistryEndpoint("", ""));
        r.assertEqualDataBoundBeans(b1, r.configRoundtrip(b1));
    }

}
