package campus.foundly.matching;

import campus.foundly.domain.ItemReport;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MatchScorer {
  public record Score(int value, String explanation) {}

  private final int category, keywords, colour, location, date, total;

  public MatchScorer(
      @Value("${foundly.matching.category:35}") int category,
      @Value("${foundly.matching.keywords:25}") int keywords,
      @Value("${foundly.matching.colour:15}") int colour,
      @Value("${foundly.matching.location:15}") int location,
      @Value("${foundly.matching.date:10}") int date) {
    this.category = category;
    this.keywords = keywords;
    this.colour = colour;
    this.location = location;
    this.date = date;
    this.total = category + keywords + colour + location + date;
    if (total <= 0
        || Math.min(Math.min(category, keywords), Math.min(colour, Math.min(location, date))) < 0)
      throw new IllegalArgumentException("Match weights must be nonnegative with a positive total");
  }

  public Score score(ItemReport a, ItemReport b) {
    if (Objects.equals(a.id, b.id)
        || a.type.equals(b.type)
        || !a.status.equals("ACTIVE")
        || !b.status.equals("ACTIVE")
        || !a.owner.campus.id.equals(b.owner.campus.id))
      return new Score(0, "Not an eligible lost/found pair");
    double points = 0;
    var reasons = new ArrayList<String>();
    if (a.category.id.equals(b.category.id)) {
      points += category;
      reasons.add("same category");
    }
    Set<String> left = tokens(a.title + " " + a.description),
        right = tokens(b.title + " " + b.description),
        intersection = new HashSet<>(left),
        union = new HashSet<>(left);
    intersection.retainAll(right);
    union.addAll(right);
    double similarity = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    points += keywords * similarity;
    if (similarity > 0) reasons.add("shared keywords");
    if (a.colour != null && !a.colour.isBlank() && a.colour.equalsIgnoreCase(b.colour)) {
      points += colour;
      reasons.add("same colour");
    }
    if (a.location.id.equals(b.location.id)) {
      points += location;
      reasons.add("same location");
    }
    long days = Math.abs(ChronoUnit.DAYS.between(a.eventDate, b.eventDate));
    points += date * Math.max(0, 1 - days / 14.0);
    if (days <= 7) reasons.add("dates within a week");
    return new Score((int) Math.round(points * 100 / total), String.join(", ", reasons));
  }

  private Set<String> tokens(String s) {
    var words = new HashSet<String>();
    for (String word : s.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
      if (word.length() > 2) words.add(word);
    return words;
  }
}
