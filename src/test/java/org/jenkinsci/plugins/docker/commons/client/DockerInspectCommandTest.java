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

import net.sf.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerInspectCommandTest {
    
    @Test
    public void test() throws IOException, InterruptedException {
        // TODO test
        // kinda tested in DockerRunCommandTest
    }
    
    @Test
    public void test_getResultAsJSON() {
        DockerInspectCommand command = new DockerInspectCommand("blah");
        command.setOut(SAMPLE_FULL_INSPECT);
        JSONArray json = command.getResultAsJSON();
        Assert.assertEquals(1, json.size());
        Assert.assertEquals("/dreamy_hopper", json.getJSONObject(0).getString("Name"));
    }
    
    
    private static final String SAMPLE_FULL_INSPECT = "[{\n" +
            "    \"Args\": [\n" +
            "        \"hello world\"\n" +
            "    ],\n" +
            "    \"Config\": {\n" +
            "        \"AttachStderr\": false,\n" +
            "        \"AttachStdin\": false,\n" +
            "        \"AttachStdout\": false,\n" +
            "        \"Cmd\": [\n" +
            "            \"echo\",\n" +
            "            \"hello world\"\n" +
            "        ],\n" +
            "        \"CpuShares\": 0,\n" +
            "        \"Cpuset\": \"\",\n" +
            "        \"Domainname\": \"\",\n" +
            "        \"Entrypoint\": null,\n" +
            "        \"Env\": [],\n" +
            "        \"ExposedPorts\": {},\n" +
            "        \"Hostname\": \"e2925ee87e15\",\n" +
            "        \"Image\": \"learn/tutorial\",\n" +
            "        \"Memory\": 0,\n" +
            "        \"MemorySwap\": 0,\n" +
            "        \"NetworkDisabled\": false,\n" +
            "        \"OnBuild\": null,\n" +
            "        \"OpenStdin\": false,\n" +
            "        \"PortSpecs\": null,\n" +
            "        \"SecurityOpt\": null,\n" +
            "        \"StdinOnce\": false,\n" +
            "        \"Tty\": false,\n" +
            "        \"User\": \"\",\n" +
            "        \"Volumes\": {},\n" +
            "        \"WorkingDir\": \"\"\n" +
            "    },\n" +
            "    \"Created\": \"2015-04-09T13:40:21.981801679Z\",\n" +
            "    \"Driver\": \"aufs\",\n" +
            "    \"ExecDriver\": \"native-0.2\",\n" +
            "    \"HostConfig\": {\n" +
            "        \"Binds\": null,\n" +
            "        \"CapAdd\": null,\n" +
            "        \"CapDrop\": null,\n" +
            "        \"ContainerIDFile\": \"\",\n" +
            "        \"Devices\": [],\n" +
            "        \"Dns\": null,\n" +
            "        \"DnsSearch\": null,\n" +
            "        \"ExtraHosts\": null,\n" +
            "        \"Links\": null,\n" +
            "        \"LxcConf\": [],\n" +
            "        \"NetworkMode\": \"bridge\",\n" +
            "        \"PortBindings\": {},\n" +
            "        \"Privileged\": false,\n" +
            "        \"PublishAllPorts\": false,\n" +
            "        \"RestartPolicy\": {\n" +
            "            \"MaximumRetryCount\": 0,\n" +
            "            \"Name\": \"\"\n" +
            "        },\n" +
            "        \"VolumesFrom\": null\n" +
            "    },\n" +
            "    \"HostnamePath\": \"/mnt/sda1/var/lib/docker/containers/e2925ee87e15c91ed914bf4033b5505d8e1f1b54adcbc9631ee08641b2bfd342/hostname\",\n" +
            "    \"HostsPath\": \"/mnt/sda1/var/lib/docker/containers/e2925ee87e15c91ed914bf4033b5505d8e1f1b54adcbc9631ee08641b2bfd342/hosts\",\n" +
            "    \"Id\": \"e2925ee87e15c91ed914bf4033b5505d8e1f1b54adcbc9631ee08641b2bfd342\",\n" +
            "    \"Image\": \"8dbd9e392a964056420e5d58ca5cc376ef18e2de93b5cc90e868a1bbc8318c1c\",\n" +
            "    \"MountLabel\": \"\",\n" +
            "    \"Name\": \"/dreamy_hopper\",\n" +
            "    \"NetworkSettings\": {\n" +
            "        \"Bridge\": \"\",\n" +
            "        \"Gateway\": \"\",\n" +
            "        \"IPAddress\": \"\",\n" +
            "        \"IPPrefixLen\": 0,\n" +
            "        \"MacAddress\": \"\",\n" +
            "        \"PortMapping\": null,\n" +
            "        \"Ports\": null\n" +
            "    },\n" +
            "    \"Path\": \"echo\",\n" +
            "    \"ProcessLabel\": \"\",\n" +
            "    \"ResolvConfPath\": \"/mnt/sda1/var/lib/docker/containers/e2925ee87e15c91ed914bf4033b5505d8e1f1b54adcbc9631ee08641b2bfd342/resolv.conf\",\n" +
            "    \"State\": {\n" +
            "        \"ExitCode\": 0,\n" +
            "        \"FinishedAt\": \"2015-04-09T13:40:22.366775538Z\",\n" +
            "        \"Paused\": false,\n" +
            "        \"Pid\": 0,\n" +
            "        \"Restarting\": false,\n" +
            "        \"Running\": false,\n" +
            "        \"StartedAt\": \"2015-04-09T13:40:22.223035189Z\"\n" +
            "    },\n" +
            "    \"Volumes\": {},\n" +
            "    \"VolumesRW\": {}\n" +
            "}\n" +
            "]";
}
