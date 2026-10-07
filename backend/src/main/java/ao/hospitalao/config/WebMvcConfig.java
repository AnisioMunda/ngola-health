package ao.hospitalao.config;

import ao.hospitalao.modules.audit.interceptor.AuditInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuditInterceptor auditInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(auditInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/auth/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**");
  }
}
