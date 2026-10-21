package campus.foundly.claim;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
  private final ClaimService service;

  public ClaimController(ClaimService service) {
    this.service = service;
  }

  @PostMapping("/report/{id}")
  Object submit(@PathVariable long id, @Valid @RequestBody ClaimService.Input input) {
    return service.submit(id, input);
  }

  @GetMapping
  Object list(
      @RequestParam(defaultValue = "false") boolean review,
      @RequestParam(defaultValue = "0") int page) {
    return service.list(review, page);
  }

  @GetMapping("/{id}/evidence")
  Object evidence(@PathVariable long id) {
    return service.evidence(id);
  }

  @PostMapping("/{id}/decision")
  Object decide(@PathVariable long id, @Valid @RequestBody ClaimService.Decision input) {
    return service.decide(id, input);
  }

  @PostMapping("/{id}/returned")
  void returned(@PathVariable long id) {
    service.returned(id);
  }
}
