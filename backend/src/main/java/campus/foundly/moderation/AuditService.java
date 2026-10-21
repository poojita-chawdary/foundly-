package campus.foundly.moderation;

import campus.foundly.common.Store;
import campus.foundly.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditService {
  private final Store store;

  public AuditService(Store store) {
    this.store = store;
  }

  public void record(UserAccount actor, String target, long id, String action, String reason) {
    var a = new ModerationAction();
    a.actor = actor;
    a.targetType = target;
    a.targetId = id;
    a.action = action;
    a.reason = reason;
    store.save(a);
  }
}
