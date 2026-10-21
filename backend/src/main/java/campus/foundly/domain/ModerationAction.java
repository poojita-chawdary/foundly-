package campus.foundly.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "moderation_actions")
public class ModerationAction extends BaseEntity {
  @ManyToOne(optional = false)
  public UserAccount actor;

  public String targetType;
  public Long targetId;
  public String action;

  @Column(length = 1000)
  public String reason;
}
