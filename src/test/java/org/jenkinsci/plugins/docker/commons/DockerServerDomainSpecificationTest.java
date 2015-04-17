package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import jenkins.model.Jenkins;
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
public class DockerServerDomainSpecificationTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configRoundTrip() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        assertThat(store, instanceOf(SystemCredentialsProvider.StoreImpl.class));
        Domain domain = new Domain("docker", "A domain for docker credentials",
                Collections.<DomainSpecification>singletonList(new DockerServerDomainSpecification()));
        store.addDomain(domain);
        j.submit(j.createWebClient().goTo("credential-store/domain/" + domain.getName() + "/configure")
                .getFormByName("config"));
        
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
