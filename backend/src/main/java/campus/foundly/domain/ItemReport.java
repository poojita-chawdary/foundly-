package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "item_reports")
public class ItemReport extends BaseEntity {
  @ManyToOne(optional = false)
  public UserAccount owner;

  public String type;
  public String title;

  @ManyToOne(optional = false)
  public Category category;

  public String colour;

  @Column(length = 2000)
  public String description;

  @ManyToOne(optional = false)
  public CampusLocation location;

  public java.time.LocalDate eventDate;
  public String status = "ACTIVE";
  public String imageKey;
}
