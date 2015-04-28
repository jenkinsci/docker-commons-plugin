package org.jenkinsci.plugins.docker.commons;

import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

/**
 * Used to mark {@link Domain} that credentials in this domain are meant to be used for
 * producing {@link DockerServerCredentials}.
 *
 * @author Stephen Connolly
 * @see DockerServerDomainSpecification
 */
public class DockerServerDomainRequirement extends DomainRequirement {
}
