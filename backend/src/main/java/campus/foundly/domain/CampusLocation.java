package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "campus_locations")
public class CampusLocation extends BaseEntity {
  @ManyToOne(optional = false)
  public Campus campus;

  public String name;
  public boolean active = true;
}
