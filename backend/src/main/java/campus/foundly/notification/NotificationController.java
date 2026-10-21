package campus.foundly.notification;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  Object list(@RequestParam(defaultValue = "0") int page) {
    return service.list(page);
  }

  @PostMapping("/{id}/read")
  void read(@PathVariable long id) {
    service.read(id);
  }
}
