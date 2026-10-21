package campus.foundly.campus;

import org.springframework.web.bind.annotation.*;

@RestController
public class CatalogController {
  private final CatalogService service;

  public CatalogController(CatalogService service) {
    this.service = service;
  }

  @GetMapping("/api/catalog")
  Object get() {
    return service.catalog();
  }
}
