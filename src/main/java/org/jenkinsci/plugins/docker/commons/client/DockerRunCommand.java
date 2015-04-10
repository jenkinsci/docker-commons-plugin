/*
 * The MIT License
 *
 * Copyright (c) 2013-2014, CloudBees, Inc.
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
package org.jenkinsci.plugins.docker.commons.client;

import org.jenkinsci.plugins.docker.commons.fingerprint.ContainerRecord;

import javax.annotation.Nonnull;
import java.util.Collections;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerRunCommand extends DockerCommand {

    private final String image;
    private String containerCmd;
    private String[] commandCmdArgs;

    public DockerRunCommand(@Nonnull String image) {
        addArgs("run");
        this.image = image;
    }

    public ContainerRecord getContainer() {        
        String out = getOut();        
        if (out == null) {
            return null;
        } else if (!isOptionSet(DockerCommandOption.DETACHED)) {
            throw new UnsupportedOperationException("Cannot get container info. Command was not run in detached mode.");
        }

        // TODO need to docker inpsect the container and get some info from it.
        
        return new ContainerRecord("", out, "", 0L, Collections.<String,String>emptyMap());
    }

    @Override
    public void preLaunch() {
        // The image name and the 
        addArgs(image);
        // Add the containerCmd (to be executed in the container), if supplied
        if (containerCmd != null) {
            addArgs(containerCmd);
            if (commandCmdArgs != null && commandCmdArgs.length > 0) {
                addArgs(commandCmdArgs);
            }
        }
    }

    public DockerRunCommand withContainerCommand(@Nonnull String command, String... args) {
        this.containerCmd = command;
        this.commandCmdArgs = args;
        return this;
    }

    public DockerRunCommand detached() {
        addArgs(DockerCommandOption.DETACHED.option());
        return this;
    }

    public DockerRunCommand withEnvVar(@Nonnull String name, @Nonnull String value) {
        return withEnvVar(name, value, false);
    }

    public DockerRunCommand withEnvVar(@Nonnull String name, @Nonnull String value, boolean masked) {
        String var = String.format("%s=%s", name, value);
        addArgs(DockerCommandOption.ENV_VAR.option());
        if (masked) {
            addMaskedArgs(var);
        } else {
            addArgs(var);
        }
        return this;
    }
}
