package com.agentboard.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Integration tests for tenant invite flows. */
class TenantInviteIntegrationTest extends AbstractAuthIntegrationTest {

  @Autowired
  private org.springframework.test.web.servlet.MockMvc mockMvc;

  @Test
  void adminCreatesInvite_newUserAccepts() throws Exception {
    InviteContext ctx = createInviteFor("invitee@example.com");

    mockMvc.perform(get("/auth/invites/" + ctx.rawToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresRegistration").value(true));

    mockMvc.perform(post("/auth/invites/" + ctx.rawToken() + "/accept")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Invitee",
                  "password": "secret456"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void identify_matchingEmail_returnsAccountExistsFalse() throws Exception {
    InviteContext ctx = createInviteFor("new-invitee@example.com");

    mockMvc.perform(post("/auth/invites/" + ctx.rawToken() + "/identify")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"new-invitee@example.com\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantName").value(ctx.tenantName()))
        .andExpect(jsonPath("$.inviteEmail").value("new-invitee@example.com"))
        .andExpect(jsonPath("$.accountExists").value(false));
  }

  @Test
  void identify_mismatchingEmail_returns403() throws Exception {
    InviteContext ctx = createInviteFor("target@example.com");

    mockMvc.perform(post("/auth/invites/" + ctx.rawToken() + "/identify")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"other@example.com\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void identify_existingUser_returnsAccountExistsTrue() throws Exception {
    registerUser("existing@example.com", "Other WS");
    InviteContext ctx = createInviteFor("existing@example.com");

    mockMvc.perform(post("/auth/invites/" + ctx.rawToken() + "/identify")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"existing@example.com\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountExists").value(true));
  }

  @Test
  void verifyCredentials_validPassword_returnsProfile() throws Exception {
    registerUser("verify@example.com", "Verify WS");
    InviteContext ctx = createInviteFor("verify@example.com");

    mockMvc.perform(post("/auth/invites/" + ctx.rawToken() + "/verify-credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "verify@example.com",
                  "password": "secret123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("User"))
        .andExpect(jsonPath("$.email").value("verify@example.com"));
  }

  @Test
  void verifyCredentials_invalidPassword_returns401() throws Exception {
    registerUser("badpass@example.com", "Badpass WS");
    InviteContext ctx = createInviteFor("badpass@example.com");

    mockMvc.perform(post("/auth/invites/" + ctx.rawToken() + "/verify-credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "badpass@example.com",
                  "password": "wrongpass1"
                }
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void existingUserAcceptsInvite() throws Exception {
    registerUser("member@example.com", "Member WS");
    InviteContext ctx = createInviteFor("member@example.com");

    mockMvc.perform(post("/auth/invites/" + ctx.rawToken() + "/accept")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "member@example.com",
                  "password": "secret123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.tenantName").value(ctx.tenantName()));
  }

  private InviteContext createInviteFor(String inviteeEmail) throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String adminEmail = "admin-" + suffix + "@example.com";
    var reg = mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Admin",
                  "email": "%s",
                  "password": "secret123",
                  "tenantName": "Invite WS %s"
                }
                """.formatted(adminEmail, suffix)))
        .andExpect(status().isCreated())
        .andReturn();

    String body = reg.getResponse().getContentAsString();
    String token = body.split("\"token\":\"")[1].split("\"")[0];
    String tenantId = body.split("\"tenantId\":\"")[1].split("\"")[0];

    var invite = mockMvc.perform(post("/auth/tenants/" + tenantId + "/invites")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + inviteeEmail + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.inviteUrl").isNotEmpty())
        .andReturn();

    String inviteUrl = invite.getResponse().getContentAsString()
        .split("\"inviteUrl\":\"")[1].split("\"")[0];
    String rawToken = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);
    return new InviteContext(rawToken, "Invite WS " + suffix);
  }

  private void registerUser(String email, String tenantName) throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "User",
                  "email": "%s",
                  "password": "secret123",
                  "tenantName": "%s"
                }
                """.formatted(email, tenantName)))
        .andExpect(status().isCreated());
  }

  private record InviteContext(String rawToken, String tenantName) {}
}
