/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
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
package org.jenkinsci.plugins.docker.commons.fingerprint;

import hudson.Util;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Fingerprint;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Shell;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collections;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerRunFingerprintFacetTest {
       
    @Rule
    public JenkinsRule rule = new JenkinsRule();
    
    @Test
    public void test_readResolve() throws Exception {
        FreeStyleProject p = createFreeStyleProjectWithFingerprints();
        FreeStyleBuild b = rule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        Fingerprint f = rule.jenkins._getFingerprint(Util.getDigestOf(singleContents[0] + "\n"));
        DockerRunFingerprintFacet facet = new DockerRunFingerprintFacet(f, System.currentTimeMillis(), "xxxxx");

        ContainerRecord r = new ContainerRecord("192.168.1.10", "cid", null, "magic", System.currentTimeMillis(), Collections.<String, String>emptyMap());
        facet.add(r);

        Assert.assertNull(r.getImageId());
        facet.readResolve();
        Assert.assertEquals("xxxxx", r.getImageId());
        
        System.out.println();
    }


    // Lifted from FingerprinterTest in Jenkins core
    private FreeStyleProject createFreeStyleProjectWithFingerprints() throws IOException, Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        addFingerprinterToProject(project);
        return project;
    }
    private void addFingerprinterToProject(AbstractProject<?, ?> project) throws Exception {
        StringBuilder targets = new StringBuilder();
        for (int i = 0; i < singleContents.length; i++) {
            if (project instanceof MatrixProject) {
                ((MatrixProject)project).getBuildersList().add(new Shell("echo " + singleContents[i] + " > " + singleFiles[i]));
            } else {
                ((FreeStyleProject)project).getBuildersList().add(new Shell("echo " + singleContents[i] + " > " + singleFiles[i]));                
            }            
            targets.append(singleFiles[i]).append(',');
        }
        project.getPublishersList().add(new Fingerprinter(targets.toString(), false));
    }
    
    private static final String[] singleContents = {
        "abcdef"
    };
    private static final String[] singleFiles = {
        "test.txt"
    };
}
