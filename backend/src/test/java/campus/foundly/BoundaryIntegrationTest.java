package campus.foundly;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import campus.foundly.auth.AuthService;
import campus.foundly.common.Store;
import campus.foundly.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BoundaryIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired Store store;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder encoder;
  UserAccount finder, claimant, outsider, moderator;

  UserAccount account(String prefix, Campus campus, String role) {
    var u = new UserAccount();
    u.name = prefix;
    u.email = prefix + UUID.randomUUID() + "@example.test";
    u.passwordHash = "unused-for-mock-authentication";
    u.campus = campus;
    u.role = role;
    return store.save(u);
  }

  @BeforeEach
  void prepare() {
    var campus = store.get(Campus.class, 1);
    finder = account("Finder", campus, "USER");
    claimant = account("Claimant", campus, "USER");
    moderator = account("Moderator", campus, "MODERATOR");
    var otherCampus = new Campus();
    otherCampus.name = "Other " + UUID.randomUUID();
    store.save(otherCampus);
    outsider = account("Outsider", otherCampus, "USER");
  }

  ItemReport report(String type) {
    var r = new ItemReport();
    r.owner = finder;
    r.type = type;
    r.title = "Green notebook";
    r.description = "A green notebook with plain cover";
    r.colour = "green";
    r.category = store.get(Category.class, 1);
    r.location = store.get(CampusLocation.class, 1);
    r.eventDate = LocalDate.now();
    store.save(r);
    store.flush();
    return r;
  }

  String body(Object value) throws Exception {
    return json.writeValueAsString(value);
  }

  long claim(ItemReport r) throws Exception {
    var result =
        mvc.perform(
                post("/api/claims/report/" + r.id)
                    .with(user(claimant.email))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body(Map.of("verification", "A private mark inside the cover"))))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  void campusBoundaryProtectsReportsClaimsAndImages() throws Exception {
    var r = report("FOUND");
    mvc.perform(get("/api/reports/" + r.id).with(user(outsider.email)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/reports/" + r.id + "/image").with(user(outsider.email)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/claims/report/" + r.id)
                .with(user(outsider.email))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("verification", "Private ownership detail"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void validImageIsReencodedAndPathIsNotTrusted() throws Exception {
    var r = report("FOUND");
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB), "png", bytes);
    var file = new MockMultipartFile("file", "../../secret.png", "image/png", bytes.toByteArray());
    mvc.perform(
            multipart("/api/reports/" + r.id + "/image")
                .file(file)
                .with(user(finder.email))
                .with(csrf()))
        .andExpect(status().isOk());
    assertTrue(r.imageKey.matches("[a-f0-9-]{36}\\.png"));
    mvc.perform(get("/api/reports/" + r.id + "/image").with(user(claimant.email)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"));
  }

  @Test
  void disguisedAndOversizedUploadsAreRejected() throws Exception {
    var r = report("FOUND");
    mvc.perform(
            multipart("/api/reports/" + r.id + "/image")
                .file(
                    new MockMultipartFile(
                        "file", "bad.png", "image/png", "not an image".getBytes()))
                .with(user(finder.email))
                .with(csrf()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            multipart("/api/reports/" + r.id + "/image")
                .file(
                    new MockMultipartFile(
                        "file", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1]))
                .with(user(finder.email))
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void onlyOwnerCanUpload() throws Exception {
    var r = report("FOUND");
    mvc.perform(
            multipart("/api/reports/" + r.id + "/image")
                .file(new MockMultipartFile("file", "small.png", "image/png", new byte[] {1, 2}))
                .with(user(claimant.email))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void ownAndLostReportsCannotBeClaimed() throws Exception {
    var found = report("FOUND");
    var lost = report("LOST");
    mvc.perform(
            post("/api/claims/report/" + found.id)
                .with(user(finder.email))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("verification", "Private identifying detail"))))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/claims/report/" + lost.id)
                .with(user(claimant.email))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("verification", "Private identifying detail"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void withdrawalIsClaimantOnlyAndTerminal() throws Exception {
    var id = claim(report("FOUND"));
    mvc.perform(
            post("/api/claims/" + id + "/decision")
                .with(user(finder.email))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("status", "WITHDRAWN"))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/claims/" + id + "/decision")
                .with(user(claimant.email))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("status", "WITHDRAWN"))))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/claims/" + id + "/decision")
                .with(user(finder.email))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("status", "REJECTED"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void hidingRejectsClaimsAndRestrictsVisibility() throws Exception {
    var r = report("FOUND");
    long claim = claim(r);
    mvc.perform(
            post("/api/admin/reports/" + r.id + "/hide")
                .with(user(moderator.email))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("reason", "Contains inappropriate personal information"))))
        .andExpect(status().isOk());
    assertEquals("REJECTED", store.get(Claim.class, claim).status);
    mvc.perform(get("/api/reports/" + r.id).with(user(claimant.email)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/reports/" + r.id).with(user(finder.email))).andExpect(status().isOk());
  }

  @Test
  void manualMembershipCannotBrowseUntilVerified() throws Exception {
    var auth = new AuthService(store, encoder, "MANUAL");
    String email = "Pending" + UUID.randomUUID() + "@example.test";
    var response =
        auth.register(
            new AuthService.Registration("Pending member", email, "test-only-long-password", 1L));
    assertEquals("PENDING", response.get("membership"));
    mvc.perform(get("/api/reports").with(user(email.toLowerCase(Locale.ROOT))))
        .andExpect(status().isForbidden());
  }

  @Test
  void closingRejectsPendingClaimAndCreatesAudit() throws Exception {
    var r = report("FOUND");
    var id = claim(r);
    mvc.perform(post("/api/reports/" + r.id + "/close").with(user(finder.email)).with(csrf()))
        .andExpect(status().isOk());
    assertEquals("REJECTED", store.get(Claim.class, id).status);
    assertTrue(
        store.count(
                "select count(a) from ModerationAction a where a.targetType='REPORT' and"
                    + " a.targetId=:id",
                Map.of("id", r.id))
            > 0);
  }
}
