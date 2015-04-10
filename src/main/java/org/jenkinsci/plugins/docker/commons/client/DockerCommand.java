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

    private final ArgumentListBuilder args = new ArgumentListBuilder();
    private String out;
    private String err;
    
    public abstract void preLaunch();

    protected DockerCommand() {
        // TODO set up PATH
        args.add("/usr/local/bin/docker");
    }

    public ArgumentListBuilder getArgs() {
        // Control mods on the args list.
        return args.clone();
    }

    public DockerCommand asUser(@Nonnull String username) {
        addArgs(DockerCommandOption.USERNAME, username);
        return this;
    }

    public DockerCommand withWorkingDir(@Nonnull String dir) {
        addArgs(DockerCommandOption.WORKING_DIR, dir);
        return this;
    }

    public DockerCommand bindHostVolume(@Nonnull String hostDir, @Nonnull String containerDir) {
        addArgs(DockerCommandOption.VOLUME, String.format("%s:%s", hostDir, containerDir));
        return this;
    }

    public DockerCommand allocatePseudoTTY() {
        addArgs(DockerCommandOption.PSEUDO_TTY);
        return this;
    }

    public void addArgs(Object... args) {
        for (Object arg : args) {
            this.args.add(arg.toString());
        }
    }

    public void addMaskedArgs(Object... args) {
        for (Object arg : args) {
            this.args.add(arg.toString(), true);
        }
    }
    
    public boolean isOptionSet(@Nonnull DockerCommandOption option) {
        for (String arg : args.toList()) {
            if (arg.equals(option.option())) {
                return true;
            }
        }
        return false;
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
