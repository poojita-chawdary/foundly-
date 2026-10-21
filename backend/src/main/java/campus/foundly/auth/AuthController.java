package campus.foundly.auth;

import campus.foundly.security.Access;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
  private final AuthService service;
  private final Access access;

  public AuthController(AuthService service, Access access) {
    this.service = service;
    this.access = access;
  }

  @GetMapping("/api/auth/csrf")
  Object csrf(CsrfToken token) {
    return java.util.Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/api/auth/register")
  Object register(@Valid @RequestBody AuthService.Registration request) {
    return service.register(request);
  }

  @GetMapping("/api/users/me")
  Object me() {
    return AuthService.profile(access.user());
  }
}
