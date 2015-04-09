package org.jenkinsci.plugins.docker.commons;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class SampleDockerRegistryBuilder extends Builder {
    /**
     * config.jelly should inline this.
     * Not meant to be instantiated and referenced externally.
     */
    private final DockerRegistryEndpoint endpoint;

    @DataBoundConstructor
    public SampleDockerRegistryBuilder(DockerRegistryEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public DockerRegistryEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // prepare the credentials to talk to this docker and make it available for docker you'll be forking
        KeyMaterial key = endpoint.materialize(build);
        try {
            // fork docker with appropriate environment to interact with this docker daemon
            return launcher.launch().cmds("docker", "push", endpoint.imageName("user/image")).envs(key.env()).join() == 0;
        } finally {
            key.close();
        }
    }
}
