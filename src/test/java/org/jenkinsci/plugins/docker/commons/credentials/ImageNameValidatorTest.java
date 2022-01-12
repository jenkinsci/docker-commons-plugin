package org.jenkinsci.plugins.docker.commons.credentials;

import hudson.util.FormValidation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * Tests various inputs to {@link ImageNameValidator#validateUserAndRepo(String)}.
 */
@RunWith(Parameterized.class)
public class ImageNameValidatorTest {

    @Parameterized.Parameters(name = "{index}:{0}") public static Object[][] data(){
        return new Object[][] {
                {"jenkinsci/workflow-demo",                                                                      FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo",                                                            FormValidation.Kind.OK},
                {"jenkinsci/workflow-demo:latest",                                                               FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo:latest",                                                     FormValidation.Kind.OK},
                {"workflow-demo:latest",                                                                         FormValidation.Kind.OK},
                {"workflow-demo",                                                                                FormValidation.Kind.OK},
                {"workflow-demo:latest@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750", FormValidation.Kind.OK},
                {"workflow-demo@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750",        FormValidation.Kind.OK},
                {":tag",                                                                                         FormValidation.Kind.ERROR},
                {"name:tag",                                                                                     FormValidation.Kind.OK},
                {"name:.tag",                                                                                    FormValidation.Kind.ERROR},
                {"name:-tag",                                                                                    FormValidation.Kind.ERROR},
                {"name:.tag.",                                                                                   FormValidation.Kind.ERROR},
                {"name:tag.",                                                                                    FormValidation.Kind.OK},
                {"name:tag-",                                                                                    FormValidation.Kind.OK},
                {"_name:tag",                                                                                    FormValidation.Kind.ERROR},
                {"na___me:tag",                                                                                  FormValidation.Kind.ERROR},
                {"na__me:tag",                                                                                   FormValidation.Kind.OK},
                {"name:tag\necho hello",                                                                         FormValidation.Kind.ERROR},
                {"name\necho hello:tag",                                                                         FormValidation.Kind.ERROR},
                {"name:tag$BUILD_NUMBER",                                                                        FormValidation.Kind.ERROR},
                {"name$BUILD_NUMBER:tag",                                                                        FormValidation.Kind.ERROR},
                {null,                                                                                           FormValidation.Kind.ERROR},
                {"",                                                                                             FormValidation.Kind.ERROR},
                {":",                                                                                            FormValidation.Kind.ERROR},
                {"  ",                                                                                           FormValidation.Kind.ERROR},

        };
    }

    private final String userAndRepo;
    private final FormValidation.Kind expected;

    public ImageNameValidatorTest(final String userAndRepo, final FormValidation.Kind expected) {
        this.userAndRepo = userAndRepo;
        this.expected = expected;
    }

    @Test
    public void test() {
        assertSame(expected, ImageNameValidator.validateUserAndRepo(userAndRepo).kind);
    }
}