package campus.foundly.matching;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class MatchingController {
  private final MatchingService service;

  public MatchingController(MatchingService service) {
    this.service = service;
  }

  @GetMapping
  Object list(@RequestParam(required = false) Long reportId) {
    return service.list(reportId);
  }

  @PostMapping("/{id}/dismiss")
  void dismiss(@PathVariable long id) {
    service.dismiss(id);
  }
}
