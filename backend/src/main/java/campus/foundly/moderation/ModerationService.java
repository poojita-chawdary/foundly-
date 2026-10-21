package campus.foundly.moderation;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import campus.foundly.matching.MatchingService;
import campus.foundly.notification.NotificationService;
import campus.foundly.security.Access;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ModerationService {
  public record Action(@NotBlank @Size(min = 5, max = 1000) String reason) {}

  public record CatalogInput(@NotBlank @Size(max = 120) String name, boolean active) {}

  private final Store store;
  private final Access access;
  private final AuditService audit;
  private final MatchingService matching;
  private final NotificationService notifications;

  public ModerationService(
      Store store,
      Access access,
      AuditService audit,
      MatchingService matching,
      NotificationService notifications) {
    this.store = store;
    this.access = access;
    this.audit = audit;
    this.matching = matching;
    this.notifications = notifications;
  }

  private UserAccount staff() {
    var u = access.user();
    access.staff(u);
    return u;
  }

  public Object overview() {
    var u = staff();
    var p = Map.<String, Object>of("campus", u.campus.id);
    return Map.of(
        "activeReports",
        store.count(
            "select count(r) from ItemReport r where r.owner.campus.id=:campus and"
                + " r.status='ACTIVE'",
            p),
        "returned",
        store.count(
            "select count(r) from ItemReport r where r.owner.campus.id=:campus and"
                + " r.status='RETURNED'",
            p),
        "pendingClaims",
        store.count(
            "select count(c) from Claim c where c.report.owner.campus.id=:campus and"
                + " c.status='PENDING'",
            p),
        "closedReports",
        store.count(
            "select count(r) from ItemReport r where r.owner.campus.id=:campus and"
                + " r.status='CLOSED'",
            p));
  }

  public Object flags(int page) {
    var u = staff();
    return store
        .list(
            ReportFlag.class,
            "select f from ReportFlag f where f.report.owner.campus.id=:campus order by f.createdAt"
                + " desc",
            Map.of("campus", u.campus.id),
            Math.max(page, 0) * 30,
            30)
        .stream()
        .map(
            f ->
                Map.of(
                    "id",
                    f.id,
                    "reportId",
                    f.report.id,
                    "title",
                    f.report.title,
                    "ownerId",
                    f.report.owner.id,
                    "reason",
                    f.reason,
                    "status",
                    f.status))
        .toList();
  }

  public void hide(long id, String reason) {
    var u = staff();
    store.lock(Campus.class, u.campus.id);
    var r = store.lock(ItemReport.class, id);
    access.campus(u, r);
    if (r.status.equals("RETURNED"))
      throw ApiException.bad("Returned reports are retained for audit.");
    r.status = "HIDDEN";
    matching.refresh(r);
    for (var c :
        store.list(
            Claim.class,
            "select c from Claim c where c.report.id=:id and c.status in ('PENDING','APPROVED')",
            Map.of("id", id))) {
      c.status = "REJECTED";
      c.reviewer = u;
      c.decidedAt = java.time.Instant.now();
      audit.record(u, "CLAIM", c.id, "REJECTED", "Listing hidden by moderation");
      notifications.send(
          c.claimant,
          "MODERATION",
          "The listing was hidden; its claims are no longer valid.",
          r.id);
    }
    audit.record(u, "REPORT", id, "HIDDEN", reason);
    notifications.send(
        r.owner, "MODERATION", "Your report was hidden following moderation review.", r.id);
  }

  public void resolveFlag(long id, String reason) {
    var u = staff();
    var f = store.get(ReportFlag.class, id);
    access.campus(u, f.report);
    f.status = "REVIEWED";
    audit.record(u, "FLAG", id, "REVIEWED", reason);
  }

  public Object users(int page) {
    var u = staff();
    return store
        .list(
            UserAccount.class,
            "select u from UserAccount u where u.campus.id=:campus order by u.createdAt desc",
            Map.of("campus", u.campus.id),
            Math.max(page, 0) * 30,
            30)
        .stream()
        .map(
            x ->
                Map.of(
                    "id",
                    x.id,
                    "name",
                    x.name,
                    "role",
                    x.role,
                    "membership",
                    x.membership,
                    "suspended",
                    x.suspended))
        .toList();
  }

  public void userAction(long id, String action, String reason) {
    var u = staff();
    access.admin(u);
    var target = store.lock(UserAccount.class, id);
    if (!target.campus.id.equals(u.campus.id)
        || target.id.equals(u.id)
        || target.role.equals("ADMIN")) throw ApiException.forbidden();
    switch (action) {
      case "suspend" -> target.suspended = true;
      case "restore" -> target.suspended = false;
      case "verify" -> target.membership = "VERIFIED";
      default -> throw ApiException.bad("Unknown user action");
    }
    audit.record(u, "USER", id, action.toUpperCase(Locale.ROOT), reason);
  }

  public Object audit(int page) {
    var u = staff();
    return store
        .list(
            ModerationAction.class,
            "select a from ModerationAction a where a.actor.campus.id=:campus order by a.createdAt"
                + " desc",
            Map.of("campus", u.campus.id),
            Math.max(page, 0) * 30,
            30)
        .stream()
        .map(
            a ->
                Map.of(
                    "id",
                    a.id,
                    "actor",
                    a.actor.name,
                    "targetType",
                    a.targetType,
                    "targetId",
                    a.targetId,
                    "action",
                    a.action,
                    "reason",
                    a.reason,
                    "createdAt",
                    a.createdAt))
        .toList();
  }

  public void catalog(String kind, Long id, CatalogInput input) {
    var u = staff();
    access.admin(u);
    if (kind.equals("categories")) {
      var c = id == null ? new Category() : store.get(Category.class, id);
      c.name = input.name();
      c.active = input.active();
      if (id == null) store.save(c);
      audit.record(u, "CATEGORY", c.id, "UPDATED", "Catalog management");
    } else if (kind.equals("locations")) {
      var c = id == null ? new CampusLocation() : store.get(CampusLocation.class, id);
      if (id != null && !c.campus.id.equals(u.campus.id)) throw ApiException.forbidden();
      c.campus = u.campus;
      c.name = input.name();
      c.active = input.active();
      if (id == null) store.save(c);
      audit.record(u, "LOCATION", c.id, "UPDATED", "Catalog management");
    } else throw ApiException.bad("Unknown catalog type");
  }
}
