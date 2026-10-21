package campus.foundly.security;

import campus.foundly.common.Store;
import campus.foundly.domain.UserAccount;
import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService users(Store store) {
    return email -> {
      var found =
          store.list(
              UserAccount.class,
              "select u from UserAccount u where u.email=:email",
              Map.of("email", email.toLowerCase(java.util.Locale.ROOT)));
      if (found.isEmpty()) throw new UsernameNotFoundException("Invalid credentials");
      var u = found.get(0);
      return User.withUsername(u.email)
          .password(u.passwordHash)
          .roles(u.role)
          .disabled(u.suspended || !u.membership.equals("VERIFIED"))
          .build();
    };
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/csrf",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/catalog",
                        "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/auth/login")
                    .usernameParameter("email")
                    .successHandler(
                        (req, res, auth) -> {
                          res.setContentType("application/json");
                          res.getWriter().write("{\"message\":\"Signed in\"}");
                        })
                    .failureHandler(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.getWriter()
                              .write(
                                  "{\"message\":\"Invalid credentials, pending membership, or"
                                      + " suspended account.\"}");
                        }))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, x) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.getWriter().write("{\"message\":\"Please sign in.\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, x) -> {
                          res.setStatus(403);
                          res.setContentType("application/json");
                          res.getWriter()
                              .write(
                                  "{\"message\":\"Permission denied or expired security token."
                                      + " Refresh and try again.\"}");
                        }))
        .requestCache(c -> c.disable());
    return http.build();
  }
}
