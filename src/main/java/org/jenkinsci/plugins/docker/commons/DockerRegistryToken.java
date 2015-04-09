package org.jenkinsci.plugins.docker.commons;

import hudson.remoting.VirtualChannel;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

/**
 * Represents an authentication token that docker(1) understands when pushing/pulling
 * from a docker registry. Obtained from {@link DockerRegistryCredentials}.
 *
 * @author Kohsuke Kawaguchi
 * @see DockerRegistryCredentials
 */
public final class DockerRegistryToken implements Serializable {
    private final String email;
    private final String token;

    public DockerRegistryToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    /**
     * Makes the credentials available locally and returns {@link KeyMaterial} that gives you the parameters
     * needed to access it.
     *
     * <p>
     * This is done by inserting the token into {@code ~/.dockercfg}
     */
    public KeyMaterial materialize(final URL endpoint, VirtualChannel target) throws InterruptedException, IOException {
        target.call(new MasterToSlaveCallable<Void, IOException>() {
            /**
             * Insert the token into {@code ~/.dockercfg}
             */
            @Override
            public Void call() throws IOException {
                File f = new File(System.getProperty("user.home"), ".dockercfg");
                JSONObject json = new JSONObject();

                synchronized (DockerRegistryToken.class) {// feeble attempt at serializing access to ~/.dockercfg
                    if (f.exists())
                        json = JSONObject.fromObject(FileUtils.readFileToString(f, "UTF-8"));

                    json.put(endpoint.toString(), new JSONObject()
                            .accumulate("auth", getToken())
                            .accumulate("email", getEmail()));

                    FileUtils.writeStringToFile(f, json.toString(2), "UTF-8");
                }

                return null;
            }
        });

        // risky to clean up ~/.dockercfg as multiple builds might try to use the same credentials
        return KeyMaterial.NULL;
    }

    private static final long serialVersionUID = 1L;
}
