package campus.foundly.matching;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import campus.foundly.notification.NotificationService;
import campus.foundly.report.ReportDto;
import campus.foundly.security.Access;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchingService {
  private final Store store;
  private final MatchScorer scorer;
  private final NotificationService notifications;
  private final Access access;
  private final int threshold;

  public MatchingService(
      Store store,
      MatchScorer scorer,
      NotificationService notifications,
      Access access,
      @Value("${foundly.matching.threshold:35}") int threshold) {
    this.store = store;
    this.scorer = scorer;
    this.notifications = notifications;
    this.access = access;
    this.threshold = threshold;
  }

  public void refresh(ItemReport report) {
    // Report writers first lock their campus, serializing match upserts within a campus.
    var existing =
        store.list(
            PotentialMatch.class,
            "select m from PotentialMatch m where m.lostReport.id=:id or m.foundReport.id=:id",
            Map.of("id", report.id));
    existing.forEach(m -> m.status = "INACTIVE");
    if (!report.status.equals("ACTIVE")) return;
    var others =
        store.list(
            ItemReport.class,
            "select r from ItemReport r where r.status='ACTIVE' and r.type<>:type and"
                + " r.owner.campus.id=:campus",
            Map.of("type", report.type, "campus", report.owner.campus.id),
            0,
            Integer.MAX_VALUE);
    for (var other : others) {
      var score = scorer.score(report, other);
      if (score.value() < threshold) continue;
      var lost = report.type.equals("LOST") ? report : other;
      var found = report.type.equals("FOUND") ? report : other;
      var m =
          existing.stream()
              .filter(x -> x.lostReport.id.equals(lost.id) && x.foundReport.id.equals(found.id))
              .findFirst()
              .orElse(null);
      boolean fresh = m == null;
      if (fresh) {
        m = new PotentialMatch();
        m.lostReport = lost;
        m.foundReport = found;
      }
      m.score = score.value();
      m.explanation = score.explanation();
      m.status = "SUGGESTED";
      if (fresh) {
        // IDENTITY IDs trigger an immediate INSERT, so initialise every required field first.
        store.save(m);
        notifications.send(
            lost.owner, "MATCH", "A possible match was found for " + lost.title, lost.id);
        if (!lost.owner.id.equals(found.owner.id))
          notifications.send(
              found.owner, "MATCH", "A possible match was found for " + found.title, found.id);
      }
    }
  }

  public Object list(Long reportId) {
    var u = access.user();
    var params = new HashMap<String, Object>();
    params.put("campus", u.campus.id);
    String query =
        "select m from PotentialMatch m where m.status='SUGGESTED' and m.lostReport.status='ACTIVE'"
            + " and m.foundReport.status='ACTIVE' and m.lostReport.owner.campus.id=:campus and not"
            + " exists (select d.id from MatchDismissal d where d.match.id=m.id and"
            + " d.user.id=:user)";
    params.put("user", u.id);
    if (reportId != null) {
      params.put("id", reportId);
      query += " and (m.lostReport.id=:id or m.foundReport.id=:id)";
    } else {
      query += " and (m.lostReport.owner.id=:user or m.foundReport.owner.id=:user)";
    }
    return store.list(PotentialMatch.class, query + " order by m.score desc", params).stream()
        .map(
            m ->
                Map.of(
                    "id",
                    m.id,
                    "score",
                    m.score,
                    "explanation",
                    m.explanation,
                    "lost",
                    ReportDto.of(m.lostReport),
                    "found",
                    ReportDto.of(m.foundReport)))
        .toList();
  }

  public void dismiss(long id) {
    var u = access.user();
    var m = store.get(PotentialMatch.class, id);
    access.campus(u, m.lostReport);
    if (!m.lostReport.owner.id.equals(u.id) && !m.foundReport.owner.id.equals(u.id))
      throw ApiException.forbidden();
    if (store.count(
            "select count(d) from MatchDismissal d where d.match.id=:m and d.user.id=:u",
            Map.of("m", id, "u", u.id))
        == 0) {
      var d = new MatchDismissal();
      d.match = m;
      d.user = u;
      store.save(d);
    }
  }
}
