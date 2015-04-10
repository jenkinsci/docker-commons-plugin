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
import org.jenkinsci.plugins.docker.commons.fingerprint.ContainerRecord;
import org.jenkinsci.plugins.docker.commons.impl.ServerKeyMaterialImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerClientTest {

    @Test
    public void test() {
        System.out.println("*** " + DockerCommandOption.DETACHED);
    }
    
    @Test
    public void test_run() throws IOException, InterruptedException {
        
        
        // Set stuff up for the test
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TaskListener taskListener = new StreamBuildListener(outputStream);
        Launcher launcher = new Launcher.LocalLauncher(taskListener);
        
        // Create a docker client
        DockerClient dockerClient = new DockerClient(launcher);        
        dockerClient.setKeyMaterial(
                new ServerKeyMaterialImpl("tcp://192.168.59.103:2376", 
                new FilePath(new File("/Users/tfennelly/.boot2docker/certs/boot2docker-vm"))));

        // Create a "run" docker command object
        RunCommand runCommand = new RunCommand("tfennelly/hello")
                .withContainerCommand("echo", "hello world")
                .detached();
        
        // Config some general settings on the command
        runCommand.allocatePseudoTTY();
        runCommand.asUser("username");
        runCommand.withWorkingDir("/home/blah");
        
        // Launch the command via the DockerClient
        int status = dockerClient.launch(runCommand);
        if (status == 0) {

            ContainerRecord container = runCommand.getContainer();
            
        } else {
            throw new RuntimeException("Failed to run docker image");            
        }        
    }
}
