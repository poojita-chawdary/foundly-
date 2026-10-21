package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "campuses")
public class Campus extends BaseEntity {
  public String name;
  public String description;
  public boolean active = true;
}
