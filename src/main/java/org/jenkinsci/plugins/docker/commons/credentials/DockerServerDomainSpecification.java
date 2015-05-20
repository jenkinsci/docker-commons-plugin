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
package org.jenkinsci.plugins.docker.commons.credentials;

import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.DomainSpecificationDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.jenkinsci.plugins.docker.commons.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A {@link DomainSpecification} that allows users to identify credential domains that are for use against docker 
 * server instances.
 *
 * <p>
 * This is more of "abstraction for future" thing as of now, because presence/absence of this in a {@link Domain}
 * doesn't affect the lookup of the credential.
 *
 * @author Stephen Connolly
 */
public class DockerServerDomainSpecification extends DomainSpecification {

    @DataBoundConstructor
    public DockerServerDomainSpecification() {
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public Result test(DomainRequirement scope) {
        if (scope instanceof DockerServerDomainRequirement) {
            // we are a very simple specification
            return Result.POSITIVE;
        }
        return Result.UNKNOWN;
    }

    /** {@inheritDoc} */
    @Extension
    public static class DescriptorImpl extends DomainSpecificationDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.DockerServerDomainSpecification_DisplayName();
        }
    }
}
