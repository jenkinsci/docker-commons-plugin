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
public class SampleDockerBuilder extends Builder {
    /**
     * config.jelly should inline this.
     * Not meant to be instantiated and referenced externally.
     */
    private final DockerServerEndpoint server;
    private final DockerRegistryEndpoint registry;

    @DataBoundConstructor
    public SampleDockerBuilder(DockerServerEndpoint server, DockerRegistryEndpoint registry) {
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
        KeyMaterial key = server.materialize(build);
        try {
            key = key.plus(registry.materialize(build));
            // fork docker with appropriate environment to interact with this docker daemon
            return launcher.launch().cmdAsSingleString("docker run ...").envs(key.env()).join() == 0;
        } finally {
            key.close();
        }
    }
}
