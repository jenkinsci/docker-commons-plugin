package org.jenkinsci.plugins.docker.commons;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class SampleDockerRegistryBuilder extends Builder {

    private final DockerRegistryEndpoint registry;

    @DataBoundConstructor
    public SampleDockerRegistryBuilder(DockerRegistryEndpoint registry) {
        this.registry = registry;
    }

    public DockerRegistryEndpoint getRegistry() {
        return registry;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // prepare the credentials to talk to this docker and make it available for docker you'll be forking
        KeyMaterial key = registry.newKeyMaterialFactory(build).materialize();
        try {
            // fork docker with appropriate environment to interact with this docker daemon
            return launcher.launch().cmds("docker", "push", registry.imageName("user/image")).envs(key.env()).join() == 0;
        } finally {
            key.close();
        }
    }

    @Extension public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override public String getDisplayName() {
            return "Sample docker-push";
        }

        @Override public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

}
