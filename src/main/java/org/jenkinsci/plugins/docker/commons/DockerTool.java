/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick.
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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * An installation of Docker.
 * If nonempty, expected to have an executable file {@code bin/docker}.
 */
public class DockerTool extends ToolInstallation implements EnvironmentSpecific<DockerTool>, NodeSpecific<DockerTool> {

    /**
     * Unqualified Docker command name, for use in case no {@link DockerTool} has been selected and thus {@link #getExecutable} cannot be called.
     */
    private static final String COMMAND = "docker";

    @DataBoundConstructor public DockerTool(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    /**
     * Gets the executable name to use for a given launcher.
     * @param launcher a launcher specifying the slave (currently unused)
     * @return {@link #COMMAND} or an absolute path
     */
    private @Nonnull String getExecutable(@Nonnull Launcher launcher) {
        String home = Util.fixEmpty(getHome());
        if (home != null) {
            return home + "/bin/" + COMMAND;
        } else {
            return COMMAND;
        }
    }

    public DockerTool forEnvironment(EnvVars environment) {
        return new DockerTool(getName(), environment.expand(getHome()), getProperties());
    }

    public DockerTool forNode(Node node, TaskListener log) throws IOException, InterruptedException {
            return new DockerTool(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override public void buildEnvVars(EnvVars env) {
        String home = Util.fixEmpty(getHome());
        if (home != null) {
            env.put("PATH+DOCKER", home + "/bin");
        }
    }

    @Extension public static class DescriptorImpl extends ToolDescriptor<DockerTool> {

        @Override public String getDisplayName() {
            return "Docker";
        }

        @Override public List<? extends ToolInstaller> getDefaultInstallers() {
            // TODO can download for example https://get.docker.com/builds/Linux/x86_64/docker-1.5.0 or https://get.docker.com/builds/Linux/i386/docker-latest and place in a bin subdir
            return super.getDefaultInstallers();
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        // TODO public FormValidation doCheckHome(@QueryParameter File value) is of marginal value since it can only check paths on the master

    }

}
