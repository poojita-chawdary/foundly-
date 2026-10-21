package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "report_flags")
public class ReportFlag extends BaseEntity {
  @ManyToOne(optional = false)
  public UserAccount reporter;

  @ManyToOne(optional = false)
  public ItemReport report;

  @Column(length = 1000)
  public String reason;

  public String status = "OPEN";
}
