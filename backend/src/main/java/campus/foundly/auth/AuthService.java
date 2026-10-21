package campus.foundly.auth;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
  public record Registration(
      @NotBlank @Size(max = 100) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotNull Long campusId) {}

  private final Store store;
  private final PasswordEncoder encoder;
  private final String mode;

  public AuthService(
      Store store, PasswordEncoder encoder, @Value("${foundly.membership-mode:OPEN}") String mode) {
    this.store = store;
    this.encoder = encoder;
    this.mode = mode;
    if (!Set.of("OPEN", "MANUAL").contains(mode))
      throw new IllegalArgumentException("Invalid membership mode");
  }

  public Map<String, Object> register(Registration r) {
    if (r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.bad("Password must fit within 72 UTF-8 bytes.");
    String email = r.email().trim().toLowerCase(Locale.ROOT);
    if (store.count(
            "select count(u) from UserAccount u where u.email=:email", Map.of("email", email))
        > 0) throw new ApiException(409, "That email cannot be registered. Try signing in.");
    var campus = store.get(Campus.class, r.campusId());
    if (!campus.active) throw ApiException.bad("Campus is not active.");
    var u = new UserAccount();
    u.name = r.name().trim();
    u.email = email;
    u.passwordHash = encoder.encode(r.password());
    u.campus = campus;
    u.membership = mode.equals("OPEN") ? "VERIFIED" : "PENDING";
    store.save(u);
    return Map.of(
        "membership",
        u.membership,
        "message",
        mode.equals("OPEN")
            ? "Account created. You can sign in."
            : "Account created. Campus staff must verify your membership before sign in.");
  }

  public static Map<String, Object> profile(UserAccount u) {
    return Map.of(
        "id",
        u.id,
        "name",
        u.name,
        "role",
        u.role,
        "campusId",
        u.campus.id,
        "campusName",
        u.campus.name,
        "membership",
        u.membership);
  }
}
