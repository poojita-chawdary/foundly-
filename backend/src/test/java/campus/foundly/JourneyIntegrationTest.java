package campus.foundly;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JourneyIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  private static String suffix = UUID.randomUUID().toString().substring(0, 8);
  private static String owner = "finder" + suffix + "@example.test",
      claimant = "owner" + suffix + "@example.test",
      stranger = "other" + suffix + "@example.test";
  private static long foundId, lostId, claimId, otherClaim;
  private static MockHttpSession finderSession, claimSession;
  private final String password = "test-only-long-password";

  private String payload(Object obj) throws Exception {
    return json.writeValueAsString(obj);
  }

  private MockHttpSession login(String email) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("email", email)
                    .param("password", password))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  private long report(String email, String type) throws Exception {
    var body =
        Map.of(
            "type",
            type,
            "title",
            "Blue water bottle",
            "categoryId",
            1,
            "colour",
            "blue",
            "description",
            "Steel blue water bottle without public identifiers",
            "locationId",
            1,
            "eventDate",
            LocalDate.now().toString());
    var result =
        mvc.perform(
                post("/api/reports")
                    .with(user(email))
                    .with(csrf())
                    .contentType("application/json")
                    .content(payload(body)))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  @Order(1)
  void registerUsersAndHashPasswords() throws Exception {
    for (var email : List.of(owner, claimant, stranger)) {
      mvc.perform(
              post("/api/auth/register")
                  .with(csrf())
                  .contentType("application/json")
                  .content(
                      payload(
                          Map.of(
                              "name",
                              "Test student",
                              "email",
                              email,
                              "password",
                              password,
                              "campusId",
                              1))))
          .andExpect(status().isOk());
    }
    String hash =
        jdbc.queryForObject("select password_hash from users where email=?", String.class, owner);
    assertNotEquals(password, hash);
    assertTrue(encoder.matches(password, hash));
  }

  @Test
  @Order(2)
  void invalidRegistrationAndCsrfAreRejected() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    payload(
                        Map.of(
                            "name", "Test", "email", "bad", "password", "short", "campusId", 1))))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @Order(3)
  void sessionLoginAndBadCredentials() throws Exception {
    finderSession = login(owner);
    claimSession = login(claimant);
    mvc.perform(get("/api/users/me").session(finderSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"));
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("email", owner)
                .param("password", "incorrect"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(4)
  void reportsRequireAuthenticationAndValidation() throws Exception {
    mvc.perform(get("/api/reports")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/reports")
                .with(user(owner))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Order(5)
  void createReportsAndMatch() throws Exception {
    foundId = report(owner, "FOUND");
    lostId = report(claimant, "LOST");
    mvc.perform(get("/api/matches").with(user(claimant)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].score").value(100));
    assertEquals(1, jdbc.queryForObject("select count(*) from matches", Integer.class));
  }

  @Test
  @Order(6)
  void searchFiltersAndPagination() throws Exception {
    mvc.perform(
            get("/api/reports")
                .with(user(owner))
                .param("type", "FOUND")
                .param("q", "bottle")
                .param("categoryId", "1")
                .param("locationId", "1")
                .param("from", LocalDate.now().minusDays(1).toString())
                .param("to", LocalDate.now().toString())
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].type").value("FOUND"));
    mvc.perform(get("/api/reports").with(user(owner)).param("q", "%_!"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  @Order(7)
  void ownershipAndDuplicateMatch() throws Exception {
    mvc.perform(post("/api/reports/" + foundId + "/close").with(user(stranger)).with(csrf()))
        .andExpect(status().isForbidden());
    String content =
        mvc.perform(get("/api/reports/" + foundId).with(user(owner)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var node = json.readTree(content);
    var body = new HashMap<String, Object>();
    for (String k :
        List.of("type", "title", "categoryId", "colour", "description", "locationId", "eventDate"))
      body.put(k, json.convertValue(node.get(k), Object.class));
    mvc.perform(
            put("/api/reports/" + foundId)
                .with(user(owner))
                .with(csrf())
                .contentType("application/json")
                .content(payload(body)))
        .andExpect(status().isOk());
    assertEquals(1, jdbc.queryForObject("select count(*) from matches", Integer.class));
  }

  @Test
  @Order(8)
  void claimsArePrivateAndDuplicatesRejected() throws Exception {
    String secret = "Private engraving: sunflower 42";
    var result =
        mvc.perform(
                post("/api/claims/report/" + foundId)
                    .with(user(claimant))
                    .with(csrf())
                    .contentType("application/json")
                    .content(payload(Map.of("verification", secret))))
            .andExpect(status().isOk())
            .andReturn();
    claimId = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    assertFalse(result.getResponse().getContentAsString().contains(secret));
    mvc.perform(
            post("/api/claims/report/" + foundId)
                .with(user(claimant))
                .with(csrf())
                .contentType("application/json")
                .content(payload(Map.of("verification", secret))))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/claims/" + claimId + "/evidence").with(user(stranger)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/claims/" + claimId + "/evidence").with(user(claimant)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/claims/" + claimId + "/evidence").with(user(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verification").value(secret));
    mvc.perform(get("/api/reports/" + foundId).with(user(stranger)))
        .andExpect(jsonPath("$.verification").doesNotExist());
  }

  @Test
  @Order(9)
  void onlyReviewerCanApprove() throws Exception {
    mvc.perform(
            post("/api/claims/" + claimId + "/decision")
                .with(user(stranger))
                .with(csrf())
                .contentType("application/json")
                .content(
                    payload(
                        Map.of(
                            "status", "APPROVED", "handoverMessage", "Campus help desk at 4pm"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @Order(10)
  void approvingOneRejectsConflicts() throws Exception {
    var result =
        mvc.perform(
                post("/api/claims/report/" + foundId)
                    .with(user(stranger))
                    .with(csrf())
                    .contentType("application/json")
                    .content(payload(Map.of("verification", "Another private ownership detail"))))
            .andExpect(status().isOk())
            .andReturn();
    otherClaim = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(
            post("/api/claims/" + claimId + "/decision")
                .with(user(owner))
                .with(csrf())
                .contentType("application/json")
                .content(
                    payload(
                        Map.of(
                            "status",
                            "APPROVED",
                            "handoverMessage",
                            "Campus help desk tomorrow at 4pm"))))
        .andExpect(status().isOk());
    assertEquals(
        "REJECTED",
        jdbc.queryForObject("select status from claims where id=?", String.class, otherClaim));
    mvc.perform(get("/api/matches").with(user(claimant)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @Order(11)
  void validReturnAndAudit() throws Exception {
    mvc.perform(post("/api/claims/" + claimId + "/returned").with(user(claimant)).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/claims/" + claimId + "/returned").with(user(owner)).with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(post("/api/claims/" + claimId + "/returned").with(user(owner)).with(csrf()))
        .andExpect(status().isBadRequest());
    assertEquals(
        "RETURNED",
        jdbc.queryForObject("select status from item_reports where id=?", String.class, foundId));
    assertTrue(
        jdbc.queryForObject(
                "select count(*) from moderation_actions where action='RETURNED'", Integer.class)
            > 0);
    mvc.perform(get("/api/reports").with(user(owner)).param("type", "FOUND"))
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  @Order(12)
  void roleEnforcedFromDatabaseNotInjectedAuthority() throws Exception {
    mvc.perform(get("/api/admin/stats").with(user(stranger).roles("ADMIN")))
        .andExpect(status().isForbidden());
    jdbc.update("update users set role='MODERATOR' where email=?", stranger);
    mvc.perform(get("/api/admin/stats").with(user(stranger))).andExpect(status().isOk());
    long ownerId = jdbc.queryForObject("select id from users where email=?", Long.class, owner);
    mvc.perform(
            post("/api/admin/users/" + ownerId + "/suspend")
                .with(user(stranger))
                .with(csrf())
                .contentType("application/json")
                .content(payload(Map.of("reason", "Testing permission boundary"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @Order(13)
  void notificationsArePrivate() throws Exception {
    var result =
        mvc.perform(get("/api/notifications").with(user(owner)))
            .andExpect(status().isOk())
            .andReturn();
    var items = json.readTree(result.getResponse().getContentAsString());
    assertTrue(items.size() > 0);
    long id = items.get(0).get("id").asLong();
    mvc.perform(post("/api/notifications/" + id + "/read").with(user(claimant)).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/notifications/" + id + "/read").with(user(owner)).with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  @Order(14)
  void suspendedSessionsLoseAccessAndLogoutWorks() throws Exception {
    jdbc.update("update users set suspended=true where email=?", owner);
    mvc.perform(get("/api/reports").session(finderSession)).andExpect(status().isForbidden());
    jdbc.update("update users set suspended=false where email=?", owner);
    mvc.perform(post("/api/auth/logout").session(finderSession).with(csrf()))
        .andExpect(status().isNoContent());
    assertTrue(finderSession.isInvalid());
  }
}
