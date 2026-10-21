package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "matches")
public class PotentialMatch extends BaseEntity {
  @ManyToOne(optional = false)
  public ItemReport lostReport;

  @ManyToOne(optional = false)
  public ItemReport foundReport;

  public int score;
  public String status = "SUGGESTED";
  public String explanation;
}
