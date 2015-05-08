package org.jenkinsci.plugins.docker.commons;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import org.kohsuke.stapler.DataBoundSetter;

public class SampleDockerBuilder extends Builder {

    private final DockerServerEndpoint server;
    private final DockerRegistryEndpoint registry;
    private String toolName;

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

    public String getToolName() {
        return toolName;
    }

    @DataBoundSetter public void setToolName(String toolName) {
        this.toolName = Util.fixEmpty(toolName);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // prepare the credentials to talk to this docker and make it available for docker you'll be forking
        KeyMaterialFactory keyMaterialFactory = server.newKeyMaterialFactory(build).plus(registry.newKeyMaterialFactory(build));
        KeyMaterial key = keyMaterialFactory.materialize();
        try {
            // fork docker with appropriate environment to interact with this docker daemon
            return launcher.launch().cmds(DockerTool.getExecutable(toolName, launcher), "info").envs(key.env()).join() == 0;
        } finally {
            key.close();
        }
    }

    @Extension public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override public String getDisplayName() {
            return "Get Docker Info";
        }

        @Override public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

}
