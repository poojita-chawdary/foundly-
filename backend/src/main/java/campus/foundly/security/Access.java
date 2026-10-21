package campus.foundly.security;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Access {
  private final Store store;

  public Access(Store store) {
    this.store = store;
  }

  public UserAccount user() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()))
      throw new ApiException(401, "Please sign in.");
    var users =
        store.list(
            UserAccount.class,
            "select u from UserAccount u where u.email=:email",
            Map.of("email", a.getName()));
    if (users.isEmpty()) throw new ApiException(401, "Please sign in.");
    var u = users.get(0);
    if (u.suspended || !"VERIFIED".equals(u.membership))
      throw new ApiException(
          403, "Your campus membership is pending or your account is suspended.");
    return u;
  }

  public boolean moderator(UserAccount u) {
    return u.role.equals("MODERATOR") || u.role.equals("ADMIN");
  }

  public void staff(UserAccount u) {
    if (!moderator(u)) throw ApiException.forbidden();
  }

  public void admin(UserAccount u) {
    if (!u.role.equals("ADMIN")) throw ApiException.forbidden();
  }

  public void campus(UserAccount u, ItemReport r) {
    if (!u.campus.id.equals(r.owner.campus.id)) throw ApiException.forbidden();
  }

  public void owner(UserAccount u, ItemReport r) {
    campus(u, r);
    if (!u.id.equals(r.owner.id)) throw ApiException.forbidden();
  }

  public void review(UserAccount u, ItemReport r) {
    campus(u, r);
    if (!u.id.equals(r.owner.id) && !moderator(u)) throw ApiException.forbidden();
  }
}
