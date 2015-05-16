/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
            return launcher.launch().cmds(DockerTool.getExecutable(toolName, build.getBuiltOn(), listener, build.getEnvironment(listener)), "info").envs(key.env()).join() == 0;
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
