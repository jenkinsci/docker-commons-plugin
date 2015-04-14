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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerClientTest {

    private DockerClient dockerClient;

    @Before
    public void setup() {
        // Set stuff up for the test
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TaskListener taskListener = new StreamBuildListener(outputStream);
        Launcher.LocalLauncher launcher = new Launcher.LocalLauncher(taskListener);

        // Create the KeyMaterial for connecting to the docker daemon
        // TODO a better way of setting the env for the test
        // E.g. currently need to add the following to you env
        //  -DDOCKER_COMMAND=/usr/local/bin/docker
        //  -DDOCKER_HOST_FOR_TEST="tcp://192.168.59.103:2376"
        //  -DDOCKER_HOST_KEY_DIR_FOR_TEST="/Users/tfennelly/.boot2docker/certs/boot2docker-vm"
        ServerKeyMaterialImpl keyMaterial = new ServerKeyMaterialImpl(System.getProperty("DOCKER_HOST_FOR_TEST"),
                new FilePath(new File(System.getProperty("DOCKER_HOST_KEY_DIR_FOR_TEST"))));

        dockerClient = new DockerClient(launcher).setKeyMaterial(keyMaterial);
    }

    @Test
    public void test_run() throws IOException, InterruptedException {
        ContainerRecord containerRecord =
                dockerClient.run("learn/tutorial", null, Collections.<String, String>emptyMap(),
                dockerClient.whoAmI(), "echo", "hello world");

        Assert.assertEquals(64, containerRecord.getContainerId().length());
        Assert.assertTrue(containerRecord.getContainerName().length() > 0);
        Assert.assertTrue(containerRecord.getHost().length() > 0);
        Assert.assertTrue(containerRecord.getCreated() > 1000000000000L);

        // Also test that the kill works and cleans up after itself
        Assert.assertNotNull(dockerClient.inspect(containerRecord.getContainerId(), ".Name"));
        dockerClient.kill(containerRecord.getContainerId());
        Assert.assertNull(dockerClient.inspect(containerRecord.getContainerId(), ".Name"));
    }
}
