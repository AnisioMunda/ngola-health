package ao.hospitalao.security.jwt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ao.hospitalao.modules.auth.service.TokenBlackListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private TokenBlackListService tokenBlackListService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void downstreamExceptionsAreNotConvertedIntoAuthenticationErrors() {
    JwtAuthFilter filter =
        new JwtAuthFilter(
            jwtService, userDetailsService, new ObjectMapper(), tokenBlackListService);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request,
                    response,
                    (servletRequest, servletResponse) -> {
                      throw new ServletException("downstream failure");
                    }))
        .isInstanceOf(ServletException.class)
        .hasMessage("downstream failure");
  }
}
