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

import hudson.EnvVars;
import hudson.Launcher;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerClient {
    
    private static final Logger LOGGER = Logger.getLogger(DockerClient.class.getName());
    
    private Launcher launcher;
    private KeyMaterial keyMaterial;

    public DockerClient(@Nonnull Launcher launcher) {
        this.launcher = launcher;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public DockerClient setKeyMaterial(KeyMaterial keyMaterial) {
        this.keyMaterial = keyMaterial;
        return this;
    }

    public KeyMaterial getKeyMaterial() {
        return keyMaterial;
    }

    public int launch(@Nonnull DockerCommand dockerCommand) throws IOException, InterruptedException {        
        EnvVars envVars = new EnvVars();
        
        // Let the impl perform any impl specific setup.
        dockerCommand.preLaunch();

        if (keyMaterial != null) {
            envVars.putAll(keyMaterial.env());
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Launcher.ProcStarter procStarter = launcher.launch();

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Executing docker command {0}", dockerCommand);
        }
        
        try {
            return procStarter.cmds(dockerCommand.args).envs(envVars).stdout(out).stderr(err).join();
        } finally {
            dockerCommand.setOut(out.toString());
            dockerCommand.setErr(err.toString());
        }
    }
    
}
