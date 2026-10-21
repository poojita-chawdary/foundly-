package campus.foundly.campus;

import campus.foundly.common.Store;
import campus.foundly.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
  private final Store store;

  public CatalogService(Store store) {
    this.store = store;
  }

  public Object catalog() {
    return Map.of(
        "campuses",
            store
                .list(
                    Campus.class,
                    "select c from Campus c where c.active=true order by c.name",
                    Map.of())
                .stream()
                .map(c -> Map.of("id", c.id, "name", c.name))
                .toList(),
        "categories",
            store
                .list(
                    Category.class,
                    "select c from Category c where c.active=true order by c.name",
                    Map.of())
                .stream()
                .map(c -> Map.of("id", c.id, "name", c.name))
                .toList(),
        "locations",
            store
                .list(
                    CampusLocation.class,
                    "select c from CampusLocation c where c.active=true order by c.name",
                    Map.of())
                .stream()
                .map(c -> Map.of("id", c.id, "name", c.name, "campusId", c.campus.id))
                .toList());
  }
}
