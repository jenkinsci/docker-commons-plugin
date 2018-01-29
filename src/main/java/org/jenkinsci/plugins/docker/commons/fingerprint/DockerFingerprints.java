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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.BulkChange;
import hudson.model.Fingerprint;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.FingerprintFacet;
import org.apache.commons.lang.StringUtils;

/**
 * Entry point into fingerprint related functionalities in Docker.
 * This class provide basic methods for both images and containers
 */
public class DockerFingerprints {
    
    private static final Logger LOGGER = Logger.getLogger(DockerFingerprints.class.getName());
    
    private DockerFingerprints() {} // no instantiation
 
    /**
     * Gets a fingerprint hash for Docker ID (image or container).
     * This method calculates image hash without retrieving a fingerprint by 
     * {@link DockerFingerprints#of(java.lang.String)}, which may be a high-cost call.
     * 
     * @param id Docker ID (image or container).
     *      Only 64-char full IDs are supported.
     * @return 32-char fingerprint hash
     * @throws IllegalArgumentException Invalid ID
     */
    public static @Nonnull String getFingerprintHash(@Nonnull String id) {

        // Remove the "sha256:" prefix, if it exists
        if (id.indexOf("sha256:") == 0) {
            id = id.substring(7);
        }

        if (id.length() != 64) {
            throw new IllegalArgumentException("Expecting 64-char full image ID, but got " + id);
        }
        return id.substring(0, 32);
    }

    /**
     * Gets {@link Fingerprint} for a given docker ID.
     * @param id Docker ID (image or container). Only 64-char full IDs are supported.
     * @return Created fingerprint or null if it is not found
     * @throws IOException Fingerprint loading error
     */
    public static Fingerprint of(@Nonnull String id) throws IOException {
        return Jenkins.getInstance().getFingerprintMap().get(getFingerprintHash(id));
    }
    
    private static @CheckForNull Fingerprint ofNoException(@Nonnull String id) {
        try {
            return of(id);
        } catch (IOException ex) { // The error is not a hazard in CheckForNull logic
            LOGGER.log(Level.WARNING, "Cannot retrieve a fingerprint for Docker id="+id, ex);
        }
        return null;
    }
    
    /**
     * @deprecated Use {@link #forImage(hudson.model.Run, java.lang.String, java.lang.String)}
     */
    public static @Nonnull Fingerprint forImage(@CheckForNull Run<?,?> run, @Nonnull String id) throws IOException {
        return forImage(run, id, null);
    }
    
    /**
     * Get or create a {@link Fingerprint} for the image.
     * @param run Origin of the fingerprint (if available)
     * @param id Image ID. Only 64-char full IDs are supported.
     * @param name Optional name of the image. If null, the image name will be
     *      constructed using the specified ID.
     * @return Fingerprint for the specified ID
     * @throws IOException Fingerprint load/save error
     * @since TODO
     */
    public static @Nonnull Fingerprint forImage(@CheckForNull Run<?,?> run, 
            @Nonnull String id, @CheckForNull String name) throws IOException {
        return forDockerInstance(run, id, name, "Docker image ");
    }
    
    /**
     * @deprecated Use {@link #forContainer(hudson.model.Run, java.lang.String, java.lang.String)}
     */
    @Deprecated
    public static @Nonnull Fingerprint forContainer(@CheckForNull Run<?,?> run, @Nonnull String id) throws IOException {
        return forContainer(run, id, null);
    }
    
    /**
     * Get or create a {@link Fingerprint} for the container.
     * @param run Origin of the fingerprint (if available)
     * @param id Container ID. Only 64-char full IDs are supported.
     * @param name Optional name of the container. If null, the container name will be
     *      constructed using the specified ID.
     * @return Fingerprint for the specified ID
     * @throws IOException Fingerprint load/save error
     * @since TODO
     */
    public static @Nonnull Fingerprint forContainer(@CheckForNull Run<?,?> run, 
            @Nonnull String id, @CheckForNull String name) throws IOException {
        return forDockerInstance(run, id, name, "Docker container ");
    }
    
    private static @Nonnull Fingerprint forDockerInstance(@CheckForNull Run<?,?> run, 
            @Nonnull String id, @CheckForNull String name, @Nonnull String prefix) throws IOException {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            throw new IOException("Jenkins instance is not ready");
        }
        final String imageName = prefix + (StringUtils.isNotBlank(name) ? name : id);
        return j.getFingerprintMap().getOrCreate(run, imageName, getFingerprintHash(id));
    }

    /**
     * Retrieves a facet from the {@link Fingerprint}.
     * The method suppresses {@link IOException} if a fingerprint loading fails.
     * @param <TFacet> Facet type to be retrieved
     * @param id Docker item ID. Only 64-char full IDs are supported
     * @param facetClass Class to be retrieved
     * @return First matching facet. Null may be returned if there is no facet
     *      or if the loading fails
     */
    public static @CheckForNull @SuppressWarnings("unchecked")
            <TFacet extends FingerprintFacet> TFacet getFacet
            (@Nonnull String id, @Nonnull Class<TFacet> facetClass) { 
        final Fingerprint fp = ofNoException(id); 
        return (fp != null) ? getFacet(fp, facetClass) : null;
    }
            
    /**
     * Retrieves a facet from the {@link Fingerprint}.
     * The method suppresses {@link IOException} if a fingerprint loading fails.
     * @param <TFacet> Facet type to be retrieved
     * @param id Docker item ID. Only 64-char full IDs are supported
     * @param facetClass Class to be retrieved
     * @return First matching facet. Null may be returned if the loading fails
     */
    @SuppressWarnings("unchecked")
    public static @Nonnull <TFacet extends FingerprintFacet> Collection<TFacet> getFacets
            (@Nonnull String id, @Nonnull Class<TFacet> facetClass) { 
        final Fingerprint fp = ofNoException(id);
        return (fp != null) ? getFacets(fp, facetClass) : Collections.<TFacet>emptySet();
    }        
    
    //TODO: deprecate and use the core's method when it's available
    /**
     * Retrieves a facet from the {@link Fingerprint}.
     * @param <TFacet> Facet type to be retrieved
     * @param fingerprint Fingerprint, which stores facets
     * @param facetClass Class to be retrieved
     * @return First matching facet.
     */
     @SuppressWarnings("unchecked")
    public static @CheckForNull <TFacet extends FingerprintFacet> TFacet getFacet
            (@Nonnull Fingerprint fingerprint, @Nonnull Class<TFacet> facetClass) {  
        for ( FingerprintFacet facet : fingerprint.getFacets()) {
            if (facetClass.isAssignableFrom(facet.getClass())) {
                return (TFacet)facet;
            }
        }
        return null;      
    }
 
    //TODO: deprecate and use the core's method when it's available
    /**
     * Retrieves facets from the {@link Fingerprint}.
     * @param <TFacet> Facet type to be retrieved
     * @param fingerprint Fingerprint, which stores facets
     * @param facetClass Facet class to be retrieved
     * @return All found facets
     */
    public static @Nonnull @SuppressWarnings("unchecked")
            <TFacet extends FingerprintFacet> Collection<TFacet> getFacets
            (@Nonnull Fingerprint fingerprint, @Nonnull Class<TFacet> facetClass) { 
        final List<TFacet> res = new LinkedList<TFacet>();
        for ( FingerprintFacet facet : fingerprint.getFacets()) {
            if (facetClass.isAssignableFrom(facet.getClass())) {
                res.add((TFacet)facet);
            }
        }
        return res;      
    }
    
    /**
     * Adds a new {@link ContainerRecord} for the specified image, creating necessary intermediate objects as it goes.
     */
    public static void addRunFacet(@Nonnull ContainerRecord record, @Nonnull Run<?,?> run) throws IOException {
        String imageId = record.getImageId();
        Fingerprint f = forImage(run, imageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerRunFingerprintFacet runFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerRunFingerprintFacet) {
                runFacet = (DockerRunFingerprintFacet) facet;
                break;
            }
        }
        BulkChange bc = new BulkChange(f);
        try {
            if (runFacet == null) {
                runFacet = new DockerRunFingerprintFacet(f, System.currentTimeMillis(), imageId);
                facets.add(runFacet);
            }
            runFacet.add(record);
            runFacet.addFor(run);
            DockerFingerprintAction.addToRun(f, imageId, run);
            bc.commit();
        } finally {
            bc.abort();
        }
    }

    /**
     * Creates a new {@link DockerAncestorFingerprintFacet} and {@link DockerDescendantFingerprintFacet} and adds a run.
     * Or adds to existing facets.
     * @param ancestorImageId the ID of the image specified in a {@code FROM} instruction, or null in case of {@code scratch} (i.e., the descendant is a base image)
     * @param descendantImageId the ID of the image which was built
     * @param run the build in which the image building occurred
     */
    public static void addFromFacet(@CheckForNull String ancestorImageId, @Nonnull String descendantImageId, @Nonnull Run<?,?> run) throws IOException {
        long timestamp = System.currentTimeMillis();
        if (ancestorImageId != null) {
            Fingerprint f = forImage(run, ancestorImageId);
            Collection<FingerprintFacet> facets = f.getFacets();
            DockerDescendantFingerprintFacet descendantFacet = null;
            for (FingerprintFacet facet : facets) {
                if (facet instanceof DockerDescendantFingerprintFacet) {
                    descendantFacet = (DockerDescendantFingerprintFacet) facet;
                    break;
                }
            }
            BulkChange bc = new BulkChange(f);
            try {
                if (descendantFacet == null) {
                    descendantFacet = new DockerDescendantFingerprintFacet(f, timestamp, ancestorImageId);
                    facets.add(descendantFacet);
                }
                descendantFacet.addDescendantImageId(descendantImageId);
                descendantFacet.addFor(run);
                DockerFingerprintAction.addToRun(f, ancestorImageId, run);
                bc.commit();
            } finally {
                bc.abort();
            }
        }
        Fingerprint f = forImage(run, descendantImageId);
        Collection<FingerprintFacet> facets = f.getFacets();
        DockerAncestorFingerprintFacet ancestorFacet = null;
        for (FingerprintFacet facet : facets) {
            if (facet instanceof DockerAncestorFingerprintFacet) {
                ancestorFacet = (DockerAncestorFingerprintFacet) facet;
                break;
            }
        }
        BulkChange bc = new BulkChange(f);
        try {
            if (ancestorFacet == null) {
                ancestorFacet = new DockerAncestorFingerprintFacet(f, timestamp, descendantImageId);
                facets.add(ancestorFacet);
            }
            if (ancestorImageId != null) {
                ancestorFacet.addAncestorImageId(ancestorImageId);
            }
            ancestorFacet.addFor(run);
            DockerFingerprintAction.addToRun(f, descendantImageId, run);
            bc.commit();
        } finally {
            bc.abort();
        }
    }

}
