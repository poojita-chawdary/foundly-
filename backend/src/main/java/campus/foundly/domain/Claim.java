package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "claims")
public class Claim extends BaseEntity {
  @ManyToOne(optional = false)
  public ItemReport report;

  @ManyToOne(optional = false)
  public UserAccount claimant;

  public String status = "PENDING";
  @ManyToOne public UserAccount reviewer;
  public java.time.Instant decidedAt;
  public String handoverMessage;
}
