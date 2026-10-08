package ao.hospitalao.config;

import ao.hospitalao.security.tenant.HospitalTenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TenantHibernateConfig {

  private final HospitalTenantIdentifierResolver tenantIdentifierResolver;

  @Bean
  HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer() {
    return properties ->
        properties.put(
            MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
  }
}
