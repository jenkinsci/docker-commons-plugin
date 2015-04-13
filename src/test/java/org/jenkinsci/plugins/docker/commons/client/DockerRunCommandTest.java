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

import org.jenkinsci.plugins.docker.commons.fingerprint.ContainerRecord;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerRunCommandTest extends AbstractDockerCommandTest {
    
    @Test
    public void test_run() throws IOException, InterruptedException {
        // Create a docker client
        DockerClient dockerClient = new DockerClient(launcher).setKeyMaterial(keyMaterial);

        // Create a "run" docker command object
        // Config some general settings on the command
        DockerRunCommand dockerRunCommand = new DockerRunCommand("learn/tutorial")
                .withContainerCommand("echo", "hello world")
                .detached()
                .allocatePseudoTTY()
                .asUser(dockerClient.whoAmI());

        //dockerRunCommand.withWorkingDir("/home/blah");
        
        // Launch the command via the DockerClient
        int status = dockerClient.launch(dockerRunCommand);
        if (status == 0) {

            ContainerRecord container = dockerRunCommand.getContainer(dockerClient);
            Assert.assertEquals(64, container.getContainerId().length());
            Assert.assertTrue(container.getContainerName().length() > 0);
            Assert.assertTrue(container.getHost().length() > 0);
            Assert.assertTrue(container.getCreated() > 0);
        } else {
            throw new RuntimeException("Failed to run docker image");            
        }        
    }
}
