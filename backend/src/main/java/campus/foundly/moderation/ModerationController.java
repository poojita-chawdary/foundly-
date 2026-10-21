package campus.foundly.moderation;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class ModerationController {
  private final ModerationService service;

  public ModerationController(ModerationService service) {
    this.service = service;
  }

  @GetMapping("/stats")
  Object overview() {
    return service.overview();
  }

  @GetMapping("/flags")
  Object flags(@RequestParam(defaultValue = "0") int page) {
    return service.flags(page);
  }

  @GetMapping("/users")
  Object users(@RequestParam(defaultValue = "0") int page) {
    return service.users(page);
  }

  @GetMapping("/audit")
  Object audit(@RequestParam(defaultValue = "0") int page) {
    return service.audit(page);
  }

  @PostMapping("/reports/{id}/hide")
  void hide(@PathVariable long id, @Valid @RequestBody ModerationService.Action input) {
    service.hide(id, input.reason());
  }

  @PostMapping("/flags/{id}/review")
  void flag(@PathVariable long id, @Valid @RequestBody ModerationService.Action input) {
    service.resolveFlag(id, input.reason());
  }

  @PostMapping("/users/{id}/{action}")
  void user(
      @PathVariable long id,
      @PathVariable String action,
      @Valid @RequestBody ModerationService.Action input) {
    service.userAction(id, action, input.reason());
  }

  @PostMapping("/catalog/{kind}")
  void add(@PathVariable String kind, @Valid @RequestBody ModerationService.CatalogInput input) {
    service.catalog(kind, null, input);
  }

  @PutMapping("/catalog/{kind}/{id}")
  void update(
      @PathVariable String kind,
      @PathVariable long id,
      @Valid @RequestBody ModerationService.CatalogInput input) {
    service.catalog(kind, id, input);
  }
}
