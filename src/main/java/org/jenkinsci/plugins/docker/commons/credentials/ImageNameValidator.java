/*
 * The MIT License
 *
 * Copyright (c) 2021, CloudBees, Inc.
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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ImageNameValidator {

    private static /*almost final*/ boolean SKIP = Boolean.getBoolean(ImageNameValidator.class.getName() + ".SKIP");

    /**
     * If the validation is set to be skipped.
     *
     * I.e. the system property <code>org.jenkinsci.plugins.docker.commons.credentials.ImageNameValidator.SKIP</code>
     * is set to <code>true</code>.
     * When this is se to true {@link #validateName(String)}, {@link #validateTag(String)} and {@link #validateUserAndRepo(String)}
     * returns {@link FormValidation#ok()} immediately without performing the validation.
     *
     * @return true if validation is skipped.
     */
    public static boolean skipped() {
        return SKIP;
    }

    /**
     * Splits a repository id namespace/name into it's four components (repo/namespace[/*],name,tag, digest)
     *
     * @param userAndRepo the repository ID namespace/name (ie. "jenkinsci/workflow-demo:latest").
     *                    The namespace can have more than one path element.
     * @return an array where position 0 is the namespace, 1 is the name and 2 is the tag and 3 is the digest.
     *         Any position could be <code>null</code>
     */
    public static @NonNull String[] splitUserAndRepo(@NonNull String userAndRepo) {
        String[] args = new String[4];
        if (StringUtils.isEmpty(userAndRepo)) {
            return args;
        }
        int slashIdx = userAndRepo.lastIndexOf('/');
        int tagIdx = userAndRepo.lastIndexOf(':');
        int digestIdx = userAndRepo.lastIndexOf('@');
        if (tagIdx == -1 && slashIdx == -1 && digestIdx == -1) {
            args[1] = userAndRepo;
        } else if (tagIdx < slashIdx) {
            //something:port/something or something/something
            args[0] = userAndRepo.substring(0, slashIdx);
            args[1] = userAndRepo.substring(slashIdx + 1);
        } else {
            if (slashIdx != -1) {
                args[0] = userAndRepo.substring(0, slashIdx);
                args[1] = userAndRepo.substring(slashIdx + 1);
            }
            if (digestIdx > 0) {
                int start = slashIdx > 0 ? slashIdx + 1 : 0;
                args[1] = userAndRepo.substring(start, digestIdx);
                tagIdx = args[1].lastIndexOf(':');
                if (tagIdx > 0 && tagIdx < args[1].length() - 1) {
                    args[2] = args[1].substring(tagIdx + 1);
                    args[1] = args[1].substring(0, tagIdx);
                }
                if (digestIdx < userAndRepo.length() - 1) {
                    args[3] = userAndRepo.substring(digestIdx + 1);
                }
            } else if (tagIdx > 0) {
                int start = slashIdx > 0 ? slashIdx + 1 : 0;
                args[1] = userAndRepo.substring(start, tagIdx);
                if (tagIdx < userAndRepo.length() - 1) {
                    args[2] = userAndRepo.substring(tagIdx + 1);
                }
            }
        }
        return args;
    }

    /**
     * Validates the string as <code>[registry/repo/]name[:tag]</code>
     * @param userAndRepo the image id
     * @return if it is valid or not, or OK if set to {@link #SKIP}.
     *
     * @see #VALID_NAME_COMPONENT
     * @see #VALID_TAG
     */
    public static @NonNull FormValidation validateUserAndRepo(@NonNull String userAndRepo) {
        if (SKIP) {
            return FormValidation.ok();
        }
        final String[] args = splitUserAndRepo(userAndRepo);
        if (StringUtils.isBlank(args[0]) && StringUtils.isBlank(args[1]) && StringUtils.isBlank(args[2])
                && StringUtils.isBlank(args[3])) {
            return FormValidation.error("Bad imageName format: %s", userAndRepo);
        }
        final FormValidation name = validateName(args[1]);
        final FormValidation tag = validateTag(args[2]);
        final FormValidation digest = validateDigest(args[3]);
        if (name.kind == FormValidation.Kind.OK && tag.kind == FormValidation.Kind.OK
                && digest.kind == FormValidation.Kind.OK) {
            return FormValidation.ok();
        }
        if (name.kind != FormValidation.Kind.OK ) {
            return name;
        }
        if (tag.kind != FormValidation.Kind.OK) {
            return tag;
        }
        if (digest.kind != FormValidation.Kind.OK) {
            return digest;
        }
        return FormValidation.aggregate(Arrays.asList(name, tag, digest));
    }

    /**
     * Calls {@link #validateUserAndRepo(String)} and if the result is not OK throws it as an exception.
     *
     * @param userAndRepo the image id
     * @throws FormValidation if not OK
     */
    public static void checkUserAndRepo(@NonNull String userAndRepo) throws FormValidation {
        final FormValidation validation = validateUserAndRepo(userAndRepo);
        if (validation.kind != FormValidation.Kind.OK) {
            throw validation;
        }
    }

    /**
     * A digest starts with 'sha256:' and must be valid ASCII and may contain
     * lowercase and digits.
     * A digest contains 71 additional characters.
     *
     * @see <a href=
     *      "https://docs.docker.com/engine/reference/commandline/images/#list-the-full-length-image-ids">docker
     *      digests</a>
     */
    public static final Pattern VALID_DIGEST = Pattern.compile("^sha256:([a-z0-9]){64}$");

    /**
     * Validates a digest is following the rules.
     *
     * If the tag is null or the empty string it is considered valid.
     *
     * @param digest the digest to validate.
     * @return the validation result
     * @see #VALID_DIGEST
     */
    public static @NonNull FormValidation validateDigest(@CheckForNull String digest) {
        if (SKIP) {
            return FormValidation.ok();
        }
        if (StringUtils.isEmpty(digest)) {
            return FormValidation.ok();
        }
        if (digest.length() != 71) {
            return FormValidation.error("Digest length != 71");
        }
        if (VALID_DIGEST.matcher(digest).matches()) {
            return FormValidation.ok();
        } else {
            return FormValidation.error("Digest must follow the pattern '%s'", VALID_DIGEST.pattern());
        }
    }

    /**
     * A tag name must be valid ASCII and may contain
     * lowercase and uppercase letters, digits, underscores, periods and dashes.
     * A tag name may not start with a period or a dash and may contain a maximum of 128 characters.
     *
     * @see <a href="https://docs.docker.com/engine/reference/commandline/tag/">docker tag</a>
     */
    public static final Pattern VALID_TAG = Pattern.compile("^[a-zA-Z0-9_]([a-zA-Z0-9_.-]){0,127}");


    /**
     * Validates a tag is following the rules.
     *
     * If the tag is null or the empty string it is considered valid.
     *
     * @param tag the tag to validate.
     * @return the validation result
     * @see #VALID_TAG
     */
    public static @NonNull FormValidation validateTag(@CheckForNull String tag) {
        if (SKIP) {
            return FormValidation.ok();
        }
        if (StringUtils.isEmpty(tag)) {
            return FormValidation.ok();
        }
        if (tag.length() > 128) {
            return FormValidation.error("Tag length > 128");
        }
        if (VALID_TAG.matcher(tag).matches()) {
            return FormValidation.ok();
        } else {
            return FormValidation.error("Tag must follow the pattern '%s'", VALID_TAG.pattern());
        }
    }

    /**
     * Calls {@link #validateTag(String)} and if not OK throws the exception.
     *
     * @param tag the tag
     * @throws FormValidation if not OK
     */
    public static void checkTag(@CheckForNull String tag) throws FormValidation {
        final FormValidation validation = validateTag(tag);
        if (validation.kind != FormValidation.Kind.OK) {
            throw validation;
        }
    }

    /**
     * Name components may contain lowercase letters, digits and separators.
     * A separator is defined as a period, one or two underscores, or one or more dashes.
     * A name component may not start or end with a separator.
     *
     * @see <a href="https://docs.docker.com/engine/reference/commandline/tag/">docker tag</a>
     */
    public static final Pattern VALID_NAME_COMPONENT = Pattern.compile("^[a-zA-Z0-9]+((\\.|_|__|-+)[a-zA-Z0-9]+)*$");

    /**
     * Validates a docker image name that it is following the rules as a single name component.
     *
     * If the name is null or the empty string it is not considered valid.
     *
     * @param name the name
     * @return the validation result
     * @see #VALID_NAME_COMPONENT
     */
    public static @NonNull FormValidation validateName(@CheckForNull String name) {
        if (SKIP) {
            return FormValidation.ok();
        }
        if (StringUtils.isEmpty(name)) {
            return FormValidation.error("Missing name.");
        }
        if (VALID_NAME_COMPONENT.matcher(name).matches()) {
            return FormValidation.ok();
        } else {
            return FormValidation.error("Name must follow the pattern '%s'", VALID_NAME_COMPONENT.pattern());
        }
    }

    /**
     * Calls {@link #validateName(String)} and if not OK throws the exception.
     *
     * @param name the name
     * @throws FormValidation if not OK
     */
    public static void checkName(String name) throws FormValidation {
        final FormValidation validation = validateName(name);
        if (validation.kind != FormValidation.Kind.OK) {
            throw validation;
        }
    }
}
