package campus.foundly.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final ReportService service;
  private final ImageService images;

  public ReportController(ReportService service, ImageService images) {
    this.service = service;
    this.images = images;
  }

  @GetMapping
  Object search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long locationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(defaultValue = "false") boolean mine) {
    return service.search(q, type, categoryId, locationId, from, to, page, size, mine);
  }

  @PostMapping
  Object create(@Valid @RequestBody ReportService.Input input) {
    return service.create(input);
  }

  @GetMapping("/{id}")
  Object detail(@PathVariable long id) {
    return service.detail(id);
  }

  @PutMapping("/{id}")
  Object update(@PathVariable long id, @Valid @RequestBody ReportService.Input input) {
    return service.update(id, input);
  }

  @PostMapping("/{id}/close")
  void close(@PathVariable long id) {
    service.close(id);
  }

  @PostMapping("/{id}/image")
  void upload(@PathVariable long id, @RequestParam MultipartFile file) throws java.io.IOException {
    images.upload(id, file);
  }

  @GetMapping("/{id}/image")
  ResponseEntity<byte[]> image(@PathVariable long id) throws java.io.IOException {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .body(images.read(id));
  }

  public record Flag(@NotBlank @Size(min = 5, max = 1000) String reason) {}

  @PostMapping("/{id}/flag")
  void flag(@PathVariable long id, @Valid @RequestBody Flag input) {
    service.flag(id, input.reason());
  }
}
