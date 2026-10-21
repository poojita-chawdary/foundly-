package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserAccount extends BaseEntity {
  public String name;

  @Column(unique = true)
  public String email;

  public String passwordHash;
  public String role = "USER";

  @ManyToOne(optional = false)
  public Campus campus;

  public String membership = "VERIFIED";
  public boolean suspended = false;
}
