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
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.awaitility.Awaitility.await;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jvnet.hudson.test.QueryUtils.waitUntilElementIsPresent;

/**
 * @author Stephen Connolly
 */
public class DockerServerDomainSpecificationTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private String getUniqueDomainName() {
        return "docker-domain-" + UUID.randomUUID().toString();
    }

    @Test
    public void configRoundTrip() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        assertThat(store, instanceOf(SystemCredentialsProvider.StoreImpl.class));
        String name = getUniqueDomainName();
        Domain domain = new Domain(name, "A domain for docker credentials",
                Collections.<DomainSpecification>singletonList(new DockerServerDomainSpecification()));
        store.addDomain(domain);

        HtmlPage page = j.createWebClient().goTo("credentials/store/system/domain/" + domain.getName());
        HtmlElement button = page.getFirstByXPath("//button[normalize-space(.)='Update domain']");
        HtmlPage page2 = button.click();
        HtmlForm form = (HtmlForm) waitUntilElementIsPresent(page, "form[id=credentials-dialog-form]");
        await().logging().until(() -> page2.getWebClient().waitForBackgroundJavaScript(1), is(0));
        j.submit(form);

        j.assertEqualDataBoundBeans(domain, byName(store.getDomains(),domain.getName()));
    }
    
    public Domain byName(List<Domain> domains, String name) {
        for (Domain d: domains) {
            if (name.equals(d.getName())) {
                return d;
            }
        }
        return null;
    }
}
