package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
  @ManyToOne(optional = false)
  public UserAccount recipient;

  public String type;
  public String message;
  public Long reportId;
  public boolean isRead = false;
}
