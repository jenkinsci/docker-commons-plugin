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

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.fingerprint.ContainerRecord;
import org.jenkinsci.plugins.docker.commons.impl.ServerKeyMaterialImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerClientTest {
    
    @Test
    public void test_run() throws IOException, InterruptedException {
                
        // Set stuff up for the test
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TaskListener taskListener = new StreamBuildListener(outputStream);
        Launcher launcher = new Launcher.LocalLauncher(taskListener);

        // Create the KeyMaterial for connecting to the docker daemon 
        // TODO a better way of setting this for the test, if there is on
        KeyMaterial keyMaterial = new ServerKeyMaterialImpl("tcp://192.168.59.103:2376",
                        new FilePath(new File("/Users/tfennelly/.boot2docker/certs/boot2docker-vm")));
        
        // Create a docker client
        DockerClient dockerClient = new DockerClient(launcher).setKeyMaterial(keyMaterial);

        // Create a "run" docker command object
        DockerRunCommand dockerRunCommand = new DockerRunCommand("learn/tutorial")
                .withContainerCommand("echo", "hello world")
                .detached();
        
        // Config some general settings on the command
        dockerRunCommand.allocatePseudoTTY();
        
        // Get the username and homedir same as how Oki Docki does it
        //dockerRunCommand.asUser("username");
        //dockerRunCommand.withWorkingDir("/home/blah");
        
        // Launch the command via the DockerClient
        int status = dockerClient.launch(dockerRunCommand);
        if (status == 0) {

            ContainerRecord container = dockerRunCommand.getContainer();
            Assert.assertEquals(65, container.getContainerId().length());
        } else {
            throw new RuntimeException("Failed to run docker image");            
        }        
    }
}
