package ao.hospitalao.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiProfileConfigurationTest {

    @Test
    void exposesOpenApiOnlyInTheDevelopmentProfile() throws Exception {
        var sources = new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"));

        assertThat(sources).hasSize(2);
        assertThat(sources.getFirst().getProperty("springdoc.api-docs.enabled")).isEqualTo(false);
        assertThat(sources.getFirst().getProperty("springdoc.swagger-ui.enabled")).isEqualTo(false);

        var development = sources.get(1);
        assertThat(development.getProperty("spring.config.activate.on-profile")).isEqualTo("dev");
        assertThat(development.getProperty("springdoc.api-docs.enabled")).isEqualTo(true);
        assertThat(development.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(true);
    }
}
