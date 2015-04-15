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
public class SampleDockerBuilder extends Builder {

    private final DockerServerEndpoint server;
    private final DockerRegistryEndpoint registry;

    @DataBoundConstructor
    public SampleDockerBuilder(DockerServerEndpoint server, DockerRegistryEndpoint registry) {
        if (server == null || registry == null) {
            throw new IllegalArgumentException();
        }
        this.server = server;
        this.registry = registry;
    }

    public DockerServerEndpoint getServer() {
        return server;
    }

    public DockerRegistryEndpoint getRegistry() {
        return registry;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // prepare the credentials to talk to this docker and make it available for docker you'll be forking
        KeyMaterialFactory keyMaterialFactory = server.newKeyMaterialFactory(build).plus(registry.newKeyMaterialFactory(build));
        KeyMaterial key = keyMaterialFactory.materialize();
        try {
            // fork docker with appropriate environment to interact with this docker daemon
            return launcher.launch().cmdAsSingleString("docker run ...").envs(key.env()).join() == 0;
        } finally {
            key.close();
        }
    }

    @Extension public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override public String getDisplayName() {
            return "Sample docker-run";
        }

        @Override public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

}
