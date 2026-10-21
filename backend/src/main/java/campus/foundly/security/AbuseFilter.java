package campus.foundly.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Per-instance IP windows; the reverse proxy should supply distributed limits in production. */
@Component
public class AbuseFilter extends OncePerRequestFilter {
  private record Window(long start, int hits) {}

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String path = req.getRequestURI();
    if (!"GET".equals(req.getMethod()) && path.startsWith("/api/")) {
      long now = System.currentTimeMillis();
      if (windows.size() > 10000)
        windows.entrySet().removeIf(e -> now - e.getValue().start > 60000);
      if (windows.size() > 20000) {
        res.setStatus(429);
        return;
      }
      String key = req.getRemoteAddr() + (path.startsWith("/api/auth/") ? ":auth" : ":write");
      Window next =
          windows.compute(
              key,
              (k, v) ->
                  v == null || now - v.start > 60000
                      ? new Window(now, 1)
                      : new Window(v.start, v.hits + 1));
      int limit = path.startsWith("/api/auth/") ? 20 : 90;
      if (next.hits > limit) {
        res.setStatus(429);
        res.setHeader("Retry-After", "60");
        res.setContentType("application/json");
        res.getWriter().write("{\"message\":\"Too many requests. Please wait a minute.\"}");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
