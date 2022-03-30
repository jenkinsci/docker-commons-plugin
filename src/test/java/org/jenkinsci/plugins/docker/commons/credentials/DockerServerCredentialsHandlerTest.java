/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick.
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

package org.jenkinsci.plugins.docker.commons.credentials;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.jenkinsci.plugins.credentialsbinding.impl.BindingStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class DockerServerCredentialsHandlerTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @Test
    public void basics() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                DockerServerCredentials c = new DockerServerCredentials(CredentialsScope.GLOBAL,
                        "docker-client-cert", "desc", "clientKey", "clientCertificate", "serverCaCertificate");
                CredentialsProvider.lookupStores(story.j.jenkins).iterator().next().addCredentials(Domain.global(), c);
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                String pipelineScript = IOUtils.toString(getTestResourceInputStream("basics-Jenkinsfile"), StandardCharsets.UTF_8);
                p.setDefinition(new CpsFlowDefinition(pipelineScript, true));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                SemaphoreStep.waitForStart("basics/1", b);
                // copy some test scripts into the workspace
                FilePath workspace = story.j.jenkins.getWorkspaceFor(p);
                copyTestResourceIntoWorkspace(workspace, "basics-step1.bat", 0755);
                copyTestResourceIntoWorkspace(workspace, "basics-step1.sh", 0755);
            }
        });
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.getItemByFullName("p", WorkflowJob.class);
                assertNotNull(p);
                WorkflowRun b = p.getBuildByNumber(1);
                assertNotNull(b);
                SemaphoreStep.success("basics/1", null);
                story.j.waitForCompletion(b);
                story.j.assertBuildStatusSuccess(b);
            }
        });
    }

    private InputStream getTestResourceInputStream(String fileName) {
        return getClass().getResourceAsStream(getClass().getSimpleName() + "/" + fileName);
    }

    private FilePath copyTestResourceIntoWorkspace(FilePath workspace, String fileName, int mask)
            throws IOException, InterruptedException {
        InputStream in = getTestResourceInputStream(fileName);
        FilePath f = workspace.child(fileName);
        f.copyFrom(in);
        f.chmod(mask);
        return f;
    }

}
