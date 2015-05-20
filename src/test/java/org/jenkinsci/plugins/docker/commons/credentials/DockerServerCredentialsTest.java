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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import hudson.security.ACL;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Stephen Connolly
 */
public class DockerServerCredentialsTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configRoundTripEmpty() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        assertThat(store, instanceOf(SystemCredentialsProvider.StoreImpl.class));
        Domain domain = new Domain("docker", "A domain for docker credentials",
                Collections.<DomainSpecification>singletonList(new DockerServerDomainSpecification()));
        DockerServerCredentials credentials = new DockerServerCredentials(CredentialsScope.GLOBAL, "foo", "desc", "", "", "");
        store.addDomain(domain, credentials);
        j.submit(j.createWebClient().goTo("credential-store/domain/" + domain.getName() + "/credential/"+credentials.getId()+"/update")
                .getFormByName("update"));
        
        j.assertEqualDataBoundBeans(credentials, CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(IdCredentials.class, j.getInstance(),
                ACL.SYSTEM, new DockerServerDomainRequirement()), CredentialsMatchers.withId(credentials.getId())));
    }
    
    @Test
    public void configRoundTripData() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        assertThat(store, instanceOf(SystemCredentialsProvider.StoreImpl.class));
        Domain domain = new Domain("docker", "A domain for docker credentials",
                Collections.<DomainSpecification>singletonList(new DockerServerDomainSpecification()));
        DockerServerCredentials credentials = new DockerServerCredentials(CredentialsScope.GLOBAL, "foo", "desc", "a", "b", "c");
        store.addDomain(domain, credentials);
        j.submit(j.createWebClient().goTo("credential-store/domain/" + domain.getName() + "/credential/"+credentials.getId()+"/update")
                .getFormByName("update"));
        
        j.assertEqualDataBoundBeans(credentials, CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(IdCredentials.class, j.getInstance(),
                ACL.SYSTEM, new DockerServerDomainRequirement()), CredentialsMatchers.withId(credentials.getId())));
    }
    
}
