package org.jenkinsci.plugins.docker.commons.credentials;

import static org.junit.jupiter.api.Assertions.assertSame;

import hudson.util.FormValidation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests various inputs to {@link ImageNameValidator#validateUserAndRepo(String)}.
 */
class ImageNameValidatorTest {

    static Object[][] data() {
        return new Object[][] {
                {"jenkinsci/workflow-demo",                                                                                          FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo",                                                                                FormValidation.Kind.OK},
                {"jenkinsci/workflow-demo:latest",                                                                                   FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo:latest",                                                                         FormValidation.Kind.OK},
                {"jenkinsci/workflow-demo@",                                                                                         FormValidation.Kind.ERROR},
                {"workflow-demo:latest",                                                                                             FormValidation.Kind.OK},
                {"workflow-demo",                                                                                                    FormValidation.Kind.OK},
                {"workflow-demo:latest@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750",                     FormValidation.Kind.OK},
                {"workflow-demo:latest@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b",                                         FormValidation.Kind.ERROR},
                {"workflow-demo@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750",                            FormValidation.Kind.OK},
                {"workflow-demo@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdB750",                            FormValidation.Kind.ERROR},
                {"workflow-demo:",                                                                                                   FormValidation.Kind.ERROR},
                {"workflow-demo:latest@",                                                                                            FormValidation.Kind.ERROR},
                {"workflow-demo@",                                                                                                   FormValidation.Kind.ERROR},
                {"jenkinsci/workflow-demo@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750",                  FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750",        FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo:latest@sha256:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750", FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo:latest@sha1:0123456789abcdef",                                                   FormValidation.Kind.OK},
                {"docker:80/jenkinsci/workflow-demo:latest@sha1:",                                                                   FormValidation.Kind.ERROR},
                {"docker:80/jenkinsci/workflow-demo@",                                                                               FormValidation.Kind.ERROR},
                {"docker:80/jenkinsci/workflow-demo:latest@",                                                                        FormValidation.Kind.ERROR},
                {":tag",                                                                                                             FormValidation.Kind.ERROR},
                {"name:tag",                                                                                                         FormValidation.Kind.OK},
                {"name:.tag",                                                                                                        FormValidation.Kind.ERROR},
                {"name:-tag",                                                                                                        FormValidation.Kind.ERROR},
                {"name:.tag.",                                                                                                       FormValidation.Kind.ERROR},
                {"name:tag.",                                                                                                        FormValidation.Kind.OK},
                {"name:tag-",                                                                                                        FormValidation.Kind.OK},
                {"_name:tag",                                                                                                        FormValidation.Kind.ERROR},
                {"na___me:tag",                                                                                                      FormValidation.Kind.ERROR},
                {"na__me:tag",                                                                                                       FormValidation.Kind.OK},
                {"name:tag\necho hello",                                                                                             FormValidation.Kind.ERROR},
                {"name\necho hello:tag",                                                                                             FormValidation.Kind.ERROR},
                {"name:tag$BUILD_NUMBER",                                                                                            FormValidation.Kind.ERROR},
                {"name$BUILD_NUMBER:tag",                                                                                            FormValidation.Kind.ERROR},
                {null,                                                                                                               FormValidation.Kind.ERROR},
                {"",                                                                                                                 FormValidation.Kind.ERROR},
                {":",                                                                                                                FormValidation.Kind.ERROR},
                {"  ",                                                                                                               FormValidation.Kind.ERROR},

                {"a@sha512:56930391cf0e1be83108422bbef43001650cfb75f64b",                                                                                     FormValidation.Kind.ERROR},
                {"a@sha512:56930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb75056930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750", FormValidation.Kind.OK},
                {"a@sha512:B6930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb75056930391cf0e1be83108422bbef43001650cfb75f64b3429928f0c5986fdb750", FormValidation.Kind.ERROR}

        };
    }

    @ParameterizedTest(name = "{index}:{0}")
    @MethodSource("data")
    void test(String userAndRepo, FormValidation.Kind expected) {
        FormValidation res = ImageNameValidator.validateUserAndRepo(userAndRepo);
        assertSame(expected, res.kind, userAndRepo + " : " + res.getMessage());
    }
}
