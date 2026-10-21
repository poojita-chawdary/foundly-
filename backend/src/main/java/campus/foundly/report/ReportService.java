package campus.foundly.report;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import campus.foundly.matching.MatchingService;
import campus.foundly.moderation.AuditService;
import campus.foundly.notification.NotificationService;
import campus.foundly.security.Access;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportService {
  public record Input(
      @NotBlank @Pattern(regexp = "LOST|FOUND") String type,
      @NotBlank @Size(max = 120) String title,
      @NotNull Long categoryId,
      @Size(max = 50) String colour,
      @NotBlank @Size(min = 10, max = 2000) String description,
      @NotNull Long locationId,
      @NotNull @PastOrPresent LocalDate eventDate) {}

  public record Page(List<ReportDto> content, long total, int page, int size) {}

  private final Store store;
  private final Access access;
  private final MatchingService matching;
  private final AuditService audit;
  private final NotificationService notifications;

  public ReportService(
      Store store,
      Access access,
      MatchingService matching,
      AuditService audit,
      NotificationService notifications) {
    this.store = store;
    this.access = access;
    this.matching = matching;
    this.audit = audit;
    this.notifications = notifications;
  }

  public ReportDto create(Input input) {
    var u = access.user();
    store.lock(Campus.class, u.campus.id);
    var r = new ItemReport();
    r.owner = u;
    apply(r, input);
    store.save(r);
    store.flush();
    matching.refresh(r);
    return ReportDto.of(r);
  }

  public ReportDto update(long id, Input input) {
    var u = access.user();
    store.lock(Campus.class, u.campus.id);
    var r = store.lock(ItemReport.class, id);
    access.owner(u, r);
    if (!r.status.equals("ACTIVE")) throw ApiException.bad("Only active reports can be edited.");
    if (!r.type.equals(input.type())) throw ApiException.bad("Report type cannot be changed.");
    apply(r, input);
    matching.refresh(r);
    return ReportDto.of(r);
  }

  private void apply(ItemReport r, Input i) {
    var category = store.get(Category.class, i.categoryId());
    var location = store.get(CampusLocation.class, i.locationId());
    if (!category.active || !location.active || !location.campus.id.equals(r.owner.campus.id))
      throw ApiException.bad("Choose an active category and a location on your campus.");
    r.type = i.type();
    r.title = i.title().trim();
    r.category = category;
    r.colour = i.colour() == null ? "" : i.colour().trim();
    r.description = i.description().trim();
    r.location = location;
    r.eventDate = i.eventDate();
  }

  public ItemReport readable(long id) {
    var u = access.user();
    var r = store.get(ItemReport.class, id);
    access.campus(u, r);
    if (r.status.equals("HIDDEN") && !u.id.equals(r.owner.id) && !access.moderator(u))
      throw new ApiException(404, "Report not found.");
    return r;
  }

  public ReportDto detail(long id) {
    return ReportDto.of(readable(id));
  }

  public void close(long id) {
    var u = access.user();
    store.lock(Campus.class, u.campus.id);
    var r = store.lock(ItemReport.class, id);
    access.owner(u, r);
    if (!r.status.equals("ACTIVE"))
      throw ApiException.bad(
          "Only active reports may be closed. Complete an approved return first.");
    r.status = "CLOSED";
    matching.refresh(r);
    for (var c :
        store.list(
            Claim.class,
            "select c from Claim c where c.report.id=:id and c.status='PENDING'",
            Map.of("id", id))) {
      c.status = "REJECTED";
      c.reviewer = u;
      c.decidedAt = java.time.Instant.now();
      audit.record(u, "CLAIM", c.id, "REJECTED", "Report closed by owner");
      notifications.send(
          c.claimant, "CLAIM", "The report was closed; your claim is now rejected.", r.id);
    }
    audit.record(u, "REPORT", r.id, "CLOSED", "Closed by owner");
  }

  public Page search(
      String q,
      String type,
      Long categoryId,
      Long locationId,
      LocalDate from,
      LocalDate to,
      int page,
      int size,
      boolean mine) {
    var u = access.user();
    if (page < 0 || size < 1 || size > 50 || page > 100000)
      throw ApiException.bad("Invalid pagination.");
    if (q != null && q.length() > 120) throw ApiException.bad("Search is too long.");
    if (type != null && !Set.of("LOST", "FOUND").contains(type))
      throw ApiException.bad("Invalid report type.");
    if (from != null && to != null && from.isAfter(to))
      throw ApiException.bad("Start date must be before end date.");
    var p = new HashMap<String, Object>();
    p.put("campus", u.campus.id);
    String where = " where r.owner.campus.id=:campus";
    if (mine) {
      where += " and r.owner.id=:owner";
      p.put("owner", u.id);
    } else where += " and r.status='ACTIVE'";
    if (q != null && !q.isBlank()) {
      where +=
          " and (lower(r.title) like :q escape '!' or lower(r.description) like :q escape '!')";
      p.put(
          "q",
          "%"
              + q.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
              + "%");
    }
    if (type != null) {
      where += " and r.type=:type";
      p.put("type", type);
    }
    if (categoryId != null) {
      where += " and r.category.id=:category";
      p.put("category", categoryId);
    }
    if (locationId != null) {
      where += " and r.location.id=:location";
      p.put("location", locationId);
    }
    if (from != null) {
      where += " and r.eventDate>=:from";
      p.put("from", from);
    }
    if (to != null) {
      where += " and r.eventDate<=:to";
      p.put("to", to);
    }
    return new Page(
        store
            .list(
                ItemReport.class,
                "select r from ItemReport r" + where + " order by r.createdAt desc, r.id desc",
                p,
                page * size,
                size)
            .stream()
            .map(ReportDto::of)
            .toList(),
        store.count("select count(r) from ItemReport r" + where, p),
        page,
        size);
  }

  public void flag(long id, String reason) {
    var u = access.user();
    var r = readable(id);
    if (store.count(
            "select count(f) from ReportFlag f where f.report.id=:r and f.reporter.id=:u",
            Map.of("r", id, "u", u.id))
        > 0) throw new ApiException(409, "You already flagged this report.");
    var f = new ReportFlag();
    f.report = r;
    f.reporter = u;
    f.reason = reason;
    store.save(f);
  }
}
