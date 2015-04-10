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

import hudson.util.ArgumentListBuilder;

import javax.annotation.Nonnull;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class DockerCommand {

    final ArgumentListBuilder args = new ArgumentListBuilder();
    private String out;
    private String err;
    
    public abstract void preLaunch();

    protected DockerCommand() {
        // TODO set up PATH
        args.add("/usr/local/bin/docker");
    }

    public DockerCommand asUser(@Nonnull String username) {
        args.add(DockerCommandOption.USERNAME.option(), username);
        return this;
    }

    public DockerCommand withWorkingDir(@Nonnull String dir) {
        args.add(DockerCommandOption.WORKING_DIR.option(), dir);
        return this;
    }

    public DockerCommand bindHostVolume(@Nonnull String hostDir, @Nonnull String containerDir) {
        args.add(DockerCommandOption.VOLUME.option(), String.format("%s:%s", hostDir, containerDir));
        return this;
    }

    public DockerCommand allocatePseudoTTY() {
        args.add(DockerCommandOption.PSEUDO_TTY.option());
        return this;
    }

    public String getOut() {
        return out;
    }

    void setOut(String out) {
        this.out = out;
    }

    public String getErr() {
        return err;
    }

    void setErr(String err) {
        this.err = err;
    }

    @Override
    public String toString() {
        return args.toString();
    }
}
