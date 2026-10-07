package ao.hospitalao.config.properties;

import ao.hospitalao.modules.financial.agt.AgtProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesValidationTest {

    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void closeValidator() throws Exception {
        validatorFactory.close();
    }

    @Test
    void acceptsValidJwtSettingsAndRejectsWeakKeysAndDurations() {
        var valid = new JwtProperties(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
            900_000,
            604_800_000
        );
        assertThat(validator.validate(valid)).isEmpty();

        var invalid = new JwtProperties("short", 0, -1);
        assertThat(validator.validate(invalid))
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("secret", "expirationMs", "refreshExpirationMs");
    }

    @Test
    void requiresAnAbsoluteHttpUrlForTheApplicationBaseUrl() {
        assertThat(validator.validate(new AppProperties("http://localhost:4200"))).isEmpty();
        assertThat(validator.validate(new AppProperties("localhost:4200")))
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("baseUrl");
    }

    @Test
    void requiresValidAgtEndpointAndSoftwareIdentity() {
        var valid = new AgtProperties();
        valid.setApiUrl("https://sandbox.example.ao");
        valid.setNif("5000413178");
        valid.setSoftwareId("HospitalAO");
        valid.setSoftwareVersion("1.0.0");
        assertThat(validator.validate(valid)).isEmpty();

        var invalid = new AgtProperties();
        invalid.setApiUrl("not-a-url");
        assertThat(validator.validate(invalid))
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("apiUrl", "nif", "softwareId", "softwareVersion");
    }
}
