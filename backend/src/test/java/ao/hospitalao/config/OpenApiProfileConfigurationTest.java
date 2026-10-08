package ao.hospitalao.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class OpenApiProfileConfigurationTest {

  @Test
  void exposesOpenApiOnlyInTheDevelopmentProfile() throws Exception {
    var sources =
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"));

    assertThat(sources).hasSize(3);
    assertThat(sources.getFirst().getProperty("springdoc.api-docs.enabled")).isEqualTo(false);
    assertThat(sources.getFirst().getProperty("springdoc.swagger-ui.enabled")).isEqualTo(false);

    var production = sources.get(1);
    assertThat(production.getProperty("spring.config.activate.on-profile")).isEqualTo("prod");
    assertThat(production.getProperty("springdoc.api-docs.enabled")).isNull();
    assertThat(production.getProperty("springdoc.swagger-ui.enabled")).isNull();

    var development = sources.get(2);
    assertThat(development.getProperty("spring.config.activate.on-profile")).isEqualTo("dev");
    assertThat(development.getProperty("springdoc.api-docs.enabled")).isEqualTo(true);
    assertThat(development.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(true);
  }
}
