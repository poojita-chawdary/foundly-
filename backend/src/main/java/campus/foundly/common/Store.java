package campus.foundly.common;

import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class Store {
  @PersistenceContext private EntityManager em;

  public <T> T get(Class<T> type, long id) {
    T value = em.find(type, id);
    if (value == null) throw new ApiException(404, "Record not found.");
    return value;
  }

  public <T> T lock(Class<T> type, long id) {
    T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    if (value == null) throw new ApiException(404, "Record not found.");
    return value;
  }

  public <T> T save(T value) {
    em.persist(value);
    return value;
  }

  public <T> List<T> list(
      Class<T> type, String jpql, Map<String, ?> params, int offset, int limit) {
    var query = em.createQuery(jpql, type);
    params.forEach(query::setParameter);
    return query.setFirstResult(offset).setMaxResults(limit).getResultList();
  }

  public <T> List<T> list(Class<T> type, String jpql, Map<String, ?> params) {
    return list(type, jpql, params, 0, 1000);
  }

  public long count(String jpql, Map<String, ?> params) {
    var q = em.createQuery(jpql, Long.class);
    params.forEach(q::setParameter);
    return q.getSingleResult();
  }

  public void flush() {
    em.flush();
  }
}
