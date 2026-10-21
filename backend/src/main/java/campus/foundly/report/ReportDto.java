package campus.foundly.report;

import campus.foundly.domain.ItemReport;
import java.time.*;

/** Explicit public projection: contact information and claim secrets never enter this DTO. */
public record ReportDto(
    Long id,
    Long ownerId,
    String type,
    String title,
    Long categoryId,
    String category,
    String colour,
    String description,
    Long locationId,
    String location,
    LocalDate eventDate,
    String status,
    String imageUrl,
    Instant createdAt,
    Instant updatedAt) {
  public static ReportDto of(ItemReport r) {
    return new ReportDto(
        r.id,
        r.owner.id,
        r.type,
        r.title,
        r.category.id,
        r.category.name,
        r.colour,
        r.description,
        r.location.id,
        r.location.name,
        r.eventDate,
        r.status,
        r.imageKey == null ? null : "/api/reports/" + r.id + "/image",
        r.createdAt,
        r.updatedAt);
  }
}
