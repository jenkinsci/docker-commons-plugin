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
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.security.ACL;
import hudson.util.Secret;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
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

        j.submit(j.createWebClient().goTo("credentials/store/system/domain/" + domain.getName() + "/credential/"+credentials.getId()+"/update")
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

        j.submit(j.createWebClient().goTo("credentials/store/system/domain/" + domain.getName() + "/credential/"+credentials.getId()+"/update")
                .getFormByName("update"));
        
        j.assertEqualDataBoundBeans(credentials, CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(IdCredentials.class, j.getInstance(),
                ACL.SYSTEM, new DockerServerDomainRequirement()), CredentialsMatchers.withId(credentials.getId())));
    }

    @Test
    public void configRoundTripUpdateCertificates() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        assertThat(store, instanceOf(SystemCredentialsProvider.StoreImpl.class));
        Domain domain = new Domain("docker", "A domain for docker credentials", Collections.singletonList(new DockerServerDomainSpecification()));
        DockerServerCredentials credentials = new DockerServerCredentials(CredentialsScope.GLOBAL, "foo", "desc", Secret.fromString("key"), "client-cert", "ca-cert");
        store.addDomain(domain, credentials);

        HtmlForm form = getUpdateForm(domain, credentials);
        for (HtmlElement button : form.getElementsByAttribute("input", "class", "secret-update-btn")) {
            button.click();
        }

        form.getTextAreaByName("_.clientKey").setText("new key");
        form.getTextAreaByName("_.clientCertificate").setText("new cert");
        form.getTextAreaByName("_.serverCaCertificate").setText("new ca cert");
        j.submit(form);

        DockerServerCredentials expected = new DockerServerCredentials(
                credentials.getScope(), credentials.getId(), credentials.getDescription(),
                Secret.fromString("new key"), "new cert", "new ca cert");
        j.assertEqualDataBoundBeans(expected, findFirstWithId(credentials.getId()));
    }

    private HtmlForm getUpdateForm(Domain domain, DockerServerCredentials credentials) throws IOException, SAXException {
        return j.createWebClient().goTo("credentials/store/system/domain/" + domain.getName() + "/credential/" + credentials.getId() + "/update")
                .getFormByName("update");
    }

    private IdCredentials findFirstWithId(String credentialsId) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(IdCredentials.class, j.getInstance(), ACL.SYSTEM, new DockerServerDomainRequirement()),
                CredentialsMatchers.withId(credentialsId));
    }
}
