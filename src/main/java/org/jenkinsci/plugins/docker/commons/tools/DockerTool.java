/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick, CloudBees Inc.
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

package org.jenkinsci.plugins.docker.commons.tools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An installation of Docker.
 * If nonempty, expected to have an executable file {@code bin/docker}.
 * Use {@code <d:selectDockerTool field="toolName" xmlns:d="/lib/docker/commons/"/>} to customize.
 */
public class DockerTool extends ToolInstallation implements EnvironmentSpecific<DockerTool>, NodeSpecific<DockerTool> {

    /**
     * Unqualified Docker command name, for use in case no {@link DockerTool} has been selected and thus {@link #getExecutable} cannot be called.
     */
    static final String COMMAND = System.getProperty("DOCKER_COMMAND", "docker"); // property overridable only for tests

    @DataBoundConstructor public DockerTool(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    /**
     * Gets the executable name to use for a given launcher.
     * Suitable for the first item in {@link ArgumentListBuilder}.
     * @param name the name of the selected tool, or null for the default
     * @param node optionally, a node (such as a slave) on which we are running Docker
     * @param listener a listener, required in case {@code node} is not null
     * @param env optionally, environment variables to use when expanding the home directory
     * @return {@code docker} or an absolute path
     */
    public static @Nonnull String getExecutable(@CheckForNull String name, @CheckForNull Node node, @Nullable TaskListener listener, @CheckForNull EnvVars env) throws IOException, InterruptedException {
        if (name != null) {
            Jenkins j = Jenkins.getInstance();
            if (j != null) {
                for (DockerTool tool : j.getDescriptorByType(DescriptorImpl.class).getInstallations()) {
                    if (tool.getName().equals(name)) {
                        if (node != null) {
                            tool = tool.forNode(node, listener);
                        }
                        if (env != null) {
                            tool = tool.forEnvironment(env);
                        }
                        String home = Util.fixEmpty(tool.getHome());
                        if (home != null) {
                            if (node != null) {
                                FilePath homeFP = node.createPath(home);
                                if (homeFP != null) {
                                    return homeFP.child("bin/docker").getRemote();
                                }
                            }
                            return home + "/bin/docker";
                        }
                    }
                }
            }
        }
        return COMMAND;
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

        @Override public DockerTool[] getInstallations() {
            load(); // TODO this ought to be automatic
            return super.getInstallations();
        }

        @Override public void setInstallations(DockerTool... installations) {
            super.setInstallations(installations);
            save(); // TODO this ought to be automatic
        }

        @Override public List<? extends ToolInstaller> getDefaultInstallers() {
            return super.getDefaultInstallers();
        }

    }

}
