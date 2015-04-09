/*
 * The MIT License
 *
 * Copyright (c) 2013-2015, CloudBees, Inc.
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

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class DockerCommand {

    final ArgumentListBuilder args = new ArgumentListBuilder();
    private String out;
    private String err;
    
    public abstract void preLaunch();

    protected DockerCommand() {
        args.add("docker");
    }

    public DockerCommand asUser(@Nonnull String username) {
        args.add(DockerCommandOptions.USERNAME.name(), username);
        return this;
    }

    public DockerCommand withWorkingDir(@Nonnull String dir) {
        args.add(DockerCommandOptions.WORKING_DIR.name(), dir);
        return this;
    }

    public DockerCommand bindHostVolume(@Nonnull String hostDir, @Nonnull String containerDir) {
        args.add(DockerCommandOptions.VOLUME.name(), String.format("%s:%s", hostDir, containerDir));
        return this;
    }

    public DockerCommand allocatePseudoTTY() {
        args.add(DockerCommandOptions.PSEUDO_TTY.name());
        return this;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }    
}
