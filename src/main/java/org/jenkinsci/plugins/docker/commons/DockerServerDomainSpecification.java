package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.DomainSpecificationDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
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
