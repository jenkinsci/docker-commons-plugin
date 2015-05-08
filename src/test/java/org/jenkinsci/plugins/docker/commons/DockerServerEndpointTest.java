package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

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
        DockerServerCredentials credentials = new DockerServerCredentials(CredentialsScope.GLOBAL, "foo", "desc", "a", "b", "c");
        store.addDomain(domain, credentials);
        DockerServerEndpoint endpoint = new DockerServerEndpoint("tcp://localhost:2736", credentials.getId());
        FilePath dotDocker = DockerServerEndpoint.dotDocker(channel);
        List<FilePath> dotDockerKids = dotDocker.list();
        int initialSize = dotDockerKids == null ? 0 : dotDockerKids.size();
        KeyMaterialFactory factory = endpoint.newKeyMaterialFactory(item, channel);
        KeyMaterial keyMaterial = factory.materialize();
        FilePath path = null;
        try {
            assertThat(keyMaterial.env().get("DOCKER_HOST", "missing"), is("tcp://localhost:2736"));
            assertThat(keyMaterial.env().get("DOCKER_TLS_VERIFY", "missing"), is("1"));
            assertThat(keyMaterial.env().get("DOCKER_CERT_PATH", "missing"), not("missing"));
            path = new FilePath(channel, keyMaterial.env().get("DOCKER_CERT_PATH", "missing"));
            assertThat(path.child("key.pem").readToString(), is("a"));
            assertThat(path.child("cert.pem").readToString(), is("b"));
            assertThat(path.child("ca.pem").readToString(), is("c"));
        } finally {
            keyMaterial.close();
        }
        assertThat(path.child("key.pem").exists(), is(false));
        assertThat(path.child("cert.pem").exists(), is(false));
        assertThat(path.child("ca.pem").exists(), is(false));
        assertThat(dotDocker.list().size(), is(initialSize));
    }
    
    
}
