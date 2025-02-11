package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import hudson.security.ACL;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.Secret;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerDomainRequirement;
import org.jenkinsci.plugins.docker.commons.tools.DockerTool;
import org.jenkinsci.plugins.docker.commons.tools.DockerToolInstaller;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class CasCTest extends RoundTripAbstractTest {

    final String resource;

    public CasCTest(final String resource) {
        this.resource = resource;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] params() {
        return new Object[][]{{"casc_bare.yaml"}, {"casc_symbols.yaml"}};
    }

    @Override
    protected void assertConfiguredAsExpected(final RestartableJenkinsRule j, final String s) {

        //The credentials
        final IdCredentials cred = CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentialsInItemGroup(IdCredentials.class, j.j.jenkins,
                ACL.SYSTEM2, Collections.singletonList(new DockerServerDomainRequirement())), CredentialsMatchers.withId("dockerx509"));
        assertNotNull(cred);
        assertThat(cred, instanceOf(DockerServerCredentials.class));
        DockerServerCredentials dCreds = (DockerServerCredentials) cred;
        assertEquals("THE CLIENT", dCreds.getClientCertificate());
        assertEquals("THE SERVER", dCreds.getServerCaCertificate());
        assertEquals("Be wewy wewy cuwiet", Secret.toString(dCreds.getClientKeySecret()));
        assertEquals("Docker X.509", dCreds.getDescription());


        //The ToolInstaller
        final DockerTool[] installations = j.j.jenkins.getDescriptorByType(DockerTool.DescriptorImpl.class).getInstallations();
        assertNotNull(installations);
        assertThat(installations, arrayWithSize(2));
        assertThat(installations, arrayContaining(
                allOf(
                        hasProperty("name", equalTo("docker-latest")),
                        hasProperty("home", nullValue())
                ),
                allOf(
                        hasProperty("name", equalTo("docker-native")),
                        hasProperty("home", equalTo("/etc/docket/docker"))
                )
        ));
        final DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = installations[0].getProperties();
        assertThat(properties, contains(instanceOf(InstallSourceProperty.class)));
        final InstallSourceProperty property = (InstallSourceProperty) properties.get(0);
        assertThat(property.installers, contains(
                allOf(
                        instanceOf(DockerToolInstaller.class),
                        hasProperty("version", equalTo("latest"))
                )
        ));

        /*
        * DockerRegistryEndpoint is not directly used in this plugin in any global config sense,
        * So it is better tested in the plugins that uses it for example docker workflow plugin:
        * https://github.com/jenkinsci/docker-workflow-plugin/blob/2ba1ac97b75a3f188e243333b31ef06d55b9221a/src/main/java/org/jenkinsci/plugins/docker/workflow/declarative/GlobalConfig.java
        */

    }

    @Override
    protected String configResource() {
        return resource;
    }

    @Override
    protected String stringInLogExpected() {
        return DockerServerCredentials.class.getName();
    }
}
