/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
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
package org.jenkinsci.plugins.docker.commons.tools;

import hudson.FilePath;
import hudson.Functions;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import hudson.tools.InstallSourceProperty;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import org.apache.commons.io.output.TeeOutputStream;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.Rule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class DockerToolInstallerTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Issue({"JENKINS-36082", "JENKINS-32790"})
    @Test
    public void smokes() throws Exception {
        Assume.assumeFalse(Functions.isWindows());
        try {
            new URL("https://get.docker.com/").openStream().close();
        } catch (IOException x) {
            Assume.assumeNoException("Cannot contact get.docker.com, perhaps test machine is not online", x);
        }
        r.jenkins.getDescriptorByType(DockerTool.DescriptorImpl.class).setInstallations(
            new DockerTool("latest", "", Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new DockerToolInstaller("", "latest"))))),
            new DockerTool("1.10.0", "", Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new DockerToolInstaller("", "1.10.0"))))));
        DumbSlave slave = r.createOnlineSlave();
        FilePath toolDir = slave.getRootPath().child("tools/org.jenkinsci.plugins.docker.commons.tools.DockerTool");
        FilePath exe10 = toolDir.child("1.10.0/bin/docker");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TaskListener l = new StreamTaskListener(new TeeOutputStream(baos, System.err));
        // Download 1.10.0 for first time:
        assertEquals(exe10.getRemote(), DockerTool.getExecutable("1.10.0", slave, l, null));
        assertTrue(exe10.exists());
        assertThat(baos.toString(), containsString(Messages.DockerToolInstaller_downloading_docker_client_("1.10.0")));
        // Next time we do not need to download:
        baos.reset();
        assertEquals(exe10.getRemote(), DockerTool.getExecutable("1.10.0", slave, l, null));
        assertTrue(exe10.exists());
        assertThat(baos.toString(), not(containsString(Messages.DockerToolInstaller_downloading_docker_client_("1.10.0"))));
        // Download latest for the first time:
        baos.reset();
        FilePath exeLatest = toolDir.child("latest/bin/docker");
        assertEquals(exeLatest.getRemote(), DockerTool.getExecutable("latest", slave, l, null));
        assertTrue(exeLatest.exists());
        assertThat(baos.toString(), containsString(Messages.DockerToolInstaller_downloading_docker_client_("latest")));
        // Next time we do not need to download:
        baos.reset();
        assertEquals(exeLatest.getRemote(), DockerTool.getExecutable("latest", slave, l, null));
        assertTrue(exeLatest.exists());
        assertThat(baos.toString(), not(containsString(Messages.DockerToolInstaller_downloading_docker_client_("latest"))));
        assertThat("we do not have any extra files in here", toolDir.list("**"), arrayContainingInAnyOrder(exe10, toolDir.child("1.10.0/.timestamp"), exeLatest, toolDir.child("latest/.timestamp")));
    }

}
