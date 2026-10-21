package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "match_dismissals")
public class MatchDismissal extends BaseEntity {
  @ManyToOne(optional = false)
  public PotentialMatch match;

  @ManyToOne(optional = false)
  public UserAccount user;
}
