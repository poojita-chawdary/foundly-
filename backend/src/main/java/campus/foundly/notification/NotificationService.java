package campus.foundly.notification;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import campus.foundly.security.Access;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
  private final Store store;
  private final Access access;

  public NotificationService(Store store, Access access) {
    this.store = store;
    this.access = access;
  }

  public void send(UserAccount user, String type, String message, Long reportId) {
    var n = new Notification();
    n.recipient = user;
    n.type = type;
    n.message = message;
    n.reportId = reportId;
    store.save(n);
  }

  public Object list(int page) {
    var u = access.user();
    return store
        .list(
            Notification.class,
            "select n from Notification n where n.recipient.id=:id order by n.createdAt desc",
            Map.of("id", u.id),
            Math.max(0, page) * 30,
            30)
        .stream()
        .map(
            n ->
                Map.of(
                    "id",
                    n.id,
                    "message",
                    n.message,
                    "reportId",
                    n.reportId,
                    "read",
                    n.isRead,
                    "createdAt",
                    n.createdAt))
        .toList();
  }

  public void read(long id) {
    var n = store.get(Notification.class, id);
    if (!n.recipient.id.equals(access.user().id)) throw ApiException.forbidden();
    n.isRead = true;
  }
}
