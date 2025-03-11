package org.jenkinsci.plugins.docker.commons;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import hudson.security.ACL;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.Secret;
import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import java.util.Collections;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerDomainRequirement;
import org.jenkinsci.plugins.docker.commons.tools.DockerTool;
import org.jenkinsci.plugins.docker.commons.tools.DockerToolInstaller;
import org.junit.jupiter.api.Nested;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class JCasCTest {

    @Nested
    @WithJenkins
    class Bare extends AbstractRoundTripTest {

        @Override
        protected void assertConfiguredAsExpected(JenkinsRule j, String configContent) {
            JCasCTest.assertConfiguredAsExpected(j);
        }

        @Override
        protected String configResource() {
            return "casc_bare.yaml";
        }

        @Override
        protected String stringInLogExpected() {
            return DockerServerCredentials.class.getName();
        }
    }

    @Nested
    @WithJenkins
    class Symbols extends AbstractRoundTripTest {

        @Override
        protected void assertConfiguredAsExpected(JenkinsRule j, String configContent) {
            JCasCTest.assertConfiguredAsExpected(j);
        }

        @Override
        protected String configResource() {
            return "casc_symbols.yaml";
        }

        @Override
        protected String stringInLogExpected() {
            return DockerServerCredentials.class.getName();
        }
    }

    private static void assertConfiguredAsExpected(JenkinsRule j) {
        // The credentials
        final IdCredentials cred = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        IdCredentials.class,
                        j.jenkins,
                        ACL.SYSTEM2,
                        Collections.singletonList(new DockerServerDomainRequirement())),
                CredentialsMatchers.withId("dockerx509"));
        assertNotNull(cred);
        assertThat(cred, instanceOf(DockerServerCredentials.class));
        DockerServerCredentials dCreds = (DockerServerCredentials) cred;
        assertEquals("THE CLIENT", dCreds.getClientCertificate());
        assertEquals("THE SERVER", dCreds.getServerCaCertificate());
        assertEquals("Be wewy wewy cuwiet", Secret.toString(dCreds.getClientKeySecret()));
        assertEquals("Docker X.509", dCreds.getDescription());

        // The ToolInstaller
        final DockerTool[] installations =
                j.jenkins.getDescriptorByType(DockerTool.DescriptorImpl.class).getInstallations();
        assertNotNull(installations);
        assertThat(installations, arrayWithSize(2));
        assertThat(
                installations,
                arrayContaining(
                        allOf(hasProperty("name", equalTo("docker-latest")), hasProperty("home", nullValue())),
                        allOf(
                                hasProperty("name", equalTo("docker-native")),
                                hasProperty("home", equalTo("/etc/docket/docker")))));
        final DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = installations[0].getProperties();
        assertThat(properties, contains(instanceOf(InstallSourceProperty.class)));
        final InstallSourceProperty property = (InstallSourceProperty) properties.get(0);
        assertThat(
                property.installers,
                contains(allOf(instanceOf(DockerToolInstaller.class), hasProperty("version", equalTo("latest")))));

        /*
         * DockerRegistryEndpoint is not directly used in this plugin in any global config sense,
         * So it is better tested in the plugins that uses it for example docker workflow plugin:
         * https://github.com/jenkinsci/docker-workflow-plugin/blob/2ba1ac97b75a3f188e243333b31ef06d55b9221a/src/main/java/org/jenkinsci/plugins/docker/workflow/declarative/GlobalConfig.java
         */

    }
}
