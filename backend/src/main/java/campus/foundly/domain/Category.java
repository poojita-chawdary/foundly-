package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {
  public String name;
  public String description;
  public boolean active = true;
}
