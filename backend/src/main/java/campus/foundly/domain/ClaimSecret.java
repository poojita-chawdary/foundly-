package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "claim_secrets")
public class ClaimSecret extends BaseEntity {
  @OneToOne(optional = false)
  public Claim claim;

  @Column(length = 2000)
  public String verification;
}
