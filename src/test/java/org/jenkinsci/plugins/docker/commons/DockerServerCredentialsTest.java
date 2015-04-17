package org.jenkinsci.plugins.docker.commons;

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
