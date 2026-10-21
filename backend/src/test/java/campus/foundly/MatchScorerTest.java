package campus.foundly;

import static org.junit.jupiter.api.Assertions.*;

import campus.foundly.domain.*;
import campus.foundly.matching.MatchScorer;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MatchScorerTest {
  private final MatchScorer scorer = new MatchScorer(35, 25, 15, 15, 10);

  private ItemReport report(long id, String type) {
    var campus = new Campus();
    campus.id = 1L;
    var owner = new UserAccount();
    owner.campus = campus;
    var c = new Category();
    c.id = 1L;
    var l = new CampusLocation();
    l.id = 1L;
    var r = new ItemReport();
    r.id = id;
    r.owner = owner;
    r.type = type;
    r.title = "blue bottle";
    r.description = "steel water bottle";
    r.colour = "Blue";
    r.category = c;
    r.location = l;
    r.eventDate = LocalDate.now();
    return r;
  }

  @Test
  void identicalDetailsReach100() {
    assertEquals(100, scorer.score(report(1, "LOST"), report(2, "FOUND")).value());
  }

  @Test
  void selfIsExcluded() {
    var r = report(1, "LOST");
    assertEquals(0, scorer.score(r, r).value());
  }

  @Test
  void sameTypeIsExcluded() {
    assertEquals(0, scorer.score(report(1, "LOST"), report(2, "LOST")).value());
  }

  @Test
  void closedIsExcluded() {
    var r = report(2, "FOUND");
    r.status = "CLOSED";
    assertEquals(0, scorer.score(report(1, "LOST"), r).value());
  }

  @Test
  void returnedIsExcluded() {
    var r = report(2, "FOUND");
    r.status = "RETURNED";
    assertEquals(0, scorer.score(report(1, "LOST"), r).value());
  }

  @Test
  void otherCampusIsExcluded() {
    var a = report(1, "LOST");
    var b = report(2, "FOUND");
    b.owner.campus.id = 2L;
    assertEquals(0, scorer.score(a, b).value());
  }

  @Test
  void dateWeightDecays() {
    var a = report(1, "LOST");
    var b = report(2, "FOUND");
    b.eventDate = a.eventDate.minusDays(14);
    assertEquals(90, scorer.score(a, b).value());
  }

  @Test
  void blankColoursDoNotMatch() {
    var a = report(1, "LOST");
    var b = report(2, "FOUND");
    a.colour = "";
    b.colour = "";
    assertEquals(85, scorer.score(a, b).value());
  }

  @Test
  void invalidWeightsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new MatchScorer(0, 0, 0, 0, 0));
  }
}
