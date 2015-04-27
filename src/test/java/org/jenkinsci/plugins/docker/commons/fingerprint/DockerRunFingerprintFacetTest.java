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

import hudson.model.Fingerprint;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DockerRunFingerprintFacetTest {
       
    @Rule
    public JenkinsRule rule = new JenkinsRule();
    
    @Test
    public void test_readResolve() throws Exception {
        FreeStyleProject p = rule.createFreeStyleProject("test");
        FreeStyleBuild b = rule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        ContainerRecord r = new ContainerRecord("192.168.1.10", "cid", null, "magic", System.currentTimeMillis(), Collections.<String, String>emptyMap());
        DockerFingerprints.addRunFacet(IMAGE_ID, r, b);
        Fingerprint fingerprint = DockerFingerprints.of(IMAGE_ID);
        DockerRunFingerprintFacet facet = (DockerRunFingerprintFacet) fingerprint.getFacets().iterator().next();

        Assert.assertNull(r.getImageId());
        facet.readResolve();
        Assert.assertEquals(IMAGE_ID, r.getImageId());
    }

    private static String IMAGE_ID = "0409d3ebf4f571d7dd2cf4b00f9d897f8af1d6d8a0f1ff791d173ba9891fd72f";    
}
