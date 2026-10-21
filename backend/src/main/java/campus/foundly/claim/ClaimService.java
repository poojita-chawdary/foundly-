package campus.foundly.claim;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import campus.foundly.matching.MatchingService;
import campus.foundly.moderation.AuditService;
import campus.foundly.notification.NotificationService;
import campus.foundly.security.Access;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClaimService {
  public record Input(@NotBlank @Size(min = 10, max = 2000) String verification) {}

  public record Decision(
      @NotBlank @Pattern(regexp = "APPROVED|REJECTED|WITHDRAWN") String status,
      @Size(max = 255) String handoverMessage) {}

  public record Summary(
      Long id,
      Long reportId,
      String title,
      Long claimantId,
      String claimantName,
      String status,
      String handoverMessage,
      Instant createdAt,
      Instant decidedAt,
      boolean canReview,
      boolean canReturn) {}

  private final Store store;
  private final Access access;
  private final NotificationService notifications;
  private final MatchingService matching;
  private final AuditService audit;

  public ClaimService(
      Store store,
      Access access,
      NotificationService notifications,
      MatchingService matching,
      AuditService audit) {
    this.store = store;
    this.access = access;
    this.notifications = notifications;
    this.matching = matching;
    this.audit = audit;
  }

  private Summary summary(Claim c, UserAccount u) {
    return new Summary(
        c.id,
        c.report.id,
        c.report.title,
        c.claimant.id,
        c.claimant.name,
        c.status,
        c.handoverMessage,
        c.createdAt,
        c.decidedAt,
        c.report.owner.id.equals(u.id) || access.moderator(u),
        c.report.owner.id.equals(u.id)
            && c.report.status.equals("CLAIMED")
            && c.status.equals("APPROVED"));
  }

  public Summary submit(long reportId, Input input) {
    var u = access.user();
    var r = store.lock(ItemReport.class, reportId);
    access.campus(u, r);
    if (!r.type.equals("FOUND") || !r.status.equals("ACTIVE"))
      throw ApiException.bad("Claims require an active found report.");
    if (r.owner.id.equals(u.id)) throw ApiException.bad("You cannot claim your own report.");
    if (store.count(
            "select count(c) from Claim c where c.report.id=:r and c.claimant.id=:u",
            Map.of("r", reportId, "u", u.id))
        > 0) throw new ApiException(409, "You already submitted a claim for this item.");
    var c = new Claim();
    c.report = r;
    c.claimant = u;
    store.save(c);
    var secret = new ClaimSecret();
    secret.claim = c;
    secret.verification = input.verification();
    store.save(secret);
    notifications.send(r.owner, "CLAIM", "A new ownership claim needs your review.", r.id);
    audit.record(u, "CLAIM", c.id, "SUBMITTED", "Private verification submitted");
    return summary(c, u);
  }

  public Object list(boolean review, int page) {
    var u = access.user();
    var params = new HashMap<String, Object>();
    params.put("u", u.id);
    params.put("campus", u.campus.id);
    String filter = "c.claimant.id=:u";
    if (review)
      filter = access.moderator(u) ? "c.report.owner.campus.id=:campus" : "c.report.owner.id=:u";
    String query = "select c from Claim c where " + filter + " order by c.createdAt desc";
    if (!query.contains(":u")) params.remove("u");
    if (!query.contains(":campus")) params.remove("campus");
    return store.list(Claim.class, query, params, Math.max(page, 0) * 30, 30).stream()
        .map(c -> summary(c, u))
        .toList();
  }

  public Object evidence(long id) {
    var c = store.get(Claim.class, id);
    access.review(access.user(), c.report);
    var secret =
        store
            .list(
                ClaimSecret.class,
                "select s from ClaimSecret s where s.claim.id=:id",
                Map.of("id", id))
            .get(0);
    return Map.of("verification", secret.verification);
  }

  public Summary decide(long id, Decision input) {
    var u = access.user();
    store.lock(Campus.class, u.campus.id);
    var initial = store.get(Claim.class, id);
    var r = store.lock(ItemReport.class, initial.report.id);
    var c = store.lock(Claim.class, id);
    access.campus(u, r);
    if (!c.status.equals("PENDING")) throw ApiException.bad("This claim has already been decided.");
    if (input.status().equals("WITHDRAWN")) {
      if (!c.claimant.id.equals(u.id)) throw ApiException.forbidden();
    } else {
      access.review(u, r);
      if (c.claimant.id.equals(u.id)) throw ApiException.forbidden();
      if (!r.status.equals("ACTIVE"))
        throw ApiException.bad("The report is no longer accepting decisions.");
    }
    if (input.status().equals("APPROVED")) {
      if (input.handoverMessage() == null || input.handoverMessage().trim().length() < 10)
        throw ApiException.bad(
            "Provide a public campus handover place and time (at least 10 characters).");
      r.status = "CLAIMED";
      c.handoverMessage = input.handoverMessage().trim();
      matching.refresh(r);
      for (var other :
          store.list(
              Claim.class,
              "select c from Claim c where c.report.id=:r and c.id<>:id and c.status='PENDING'",
              Map.of("r", r.id, "id", c.id))) {
        other.status = "REJECTED";
        other.reviewer = u;
        other.decidedAt = Instant.now();
        audit.record(u, "CLAIM", other.id, "REJECTED", "Another claim approved");
        notifications.send(
            other.claimant, "CLAIM", "Another claim was approved for this item.", r.id);
      }
    }
    c.status = input.status();
    c.reviewer = u;
    c.decidedAt = Instant.now();
    audit.record(u, "CLAIM", c.id, c.status, "Claim decision");
    notifications.send(
        c.claimant, "CLAIM", "Your claim is now " + c.status.toLowerCase(Locale.ROOT) + ".", r.id);
    if (input.status().equals("WITHDRAWN"))
      notifications.send(r.owner, "CLAIM", "A claimant withdrew their claim.", r.id);
    return summary(c, u);
  }

  public void returned(long id) {
    var u = access.user();
    store.lock(Campus.class, u.campus.id);
    var c = store.get(Claim.class, id);
    var r = store.lock(ItemReport.class, c.report.id);
    access.owner(u, r);
    if (!c.status.equals("APPROVED") || !r.status.equals("CLAIMED"))
      throw ApiException.bad("An approved claim is required to confirm a return.");
    r.status = "RETURNED";
    matching.refresh(r);
    audit.record(u, "REPORT", r.id, "RETURNED", "Owner confirmed safe handover");
    notifications.send(c.claimant, "RETURNED", "The return has been confirmed. Thank you!", r.id);
    notifications.send(
        r.owner, "RETURNED", "Return recorded. Thanks for helping your campus.", r.id);
  }
}
