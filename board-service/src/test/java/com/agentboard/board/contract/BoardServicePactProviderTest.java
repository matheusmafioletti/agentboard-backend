package com.agentboard.board.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.agentboard.board.domain.Board;
import com.agentboard.board.domain.FeatureCard;
import com.agentboard.board.domain.Stage;
import com.agentboard.board.domain.Task;
import com.agentboard.board.domain.TenantApiKey;
import com.agentboard.board.repository.BoardRepository;
import com.agentboard.board.repository.ColumnDefRepository;
import com.agentboard.board.repository.FeatureCardRepository;
import com.agentboard.board.repository.TaskRepository;
import com.agentboard.board.repository.TenantApiKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies that board-service fulfils the Pact contracts defined by agentboard-mcp-server.
 *
 * <p>The pact file is read from {@code repos/agentboard-mcp-server/pacts/} — run
 * {@code npm test} in the MCP server repo first to generate the contract.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Provider("board-service")
@Consumer("agentboard-mcp-server")
@PactFolder("../agentboard-mcp-server/pacts")
class BoardServicePactProviderTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  @LocalServerPort
  int port;

  @Autowired
  BoardRepository boardRepository;

  @Autowired
  TenantApiKeyRepository tenantApiKeyRepository;

  @Autowired
  FeatureCardRepository featureCardRepository;

  @Autowired
  TaskRepository taskRepository;

  @Autowired
  ColumnDefRepository columnDefRepository;

  private static final String TEST_API_KEY = "test-api-key";
  private static final String TENANT_B_KEY = "tenant-b-api-key";
  private static final UUID TENANT_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TENANT_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID FEATURE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TASK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @BeforeEach
  void setTarget(PactVerificationContext context) {
    context.setTarget(new HttpTestTarget("localhost", port));
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @State("Tenant with valid API key exists")
  void tenantWithValidApiKeyExists() {
    ensureBoardForTenant(TENANT_A_ID);
    storeApiKey(TENANT_A_ID, TEST_API_KEY);
  }

  @State("Feature card with id \"00000000-0000-0000-0000-000000000001\" exists for tenant")
  void featureCardExists() {
    ensureBoardForTenant(TENANT_A_ID);
    storeApiKey(TENANT_A_ID, TEST_API_KEY);
    ensureFeatureCard(TENANT_A_ID, FEATURE_UUID);
  }

  @State("Feature card with id \"00000000-0000-0000-0000-000000000001\" is in BACKLOG")
  void featureCardInBacklog() {
    featureCardExists();
  }

  @State("Feature card with id \"00000000-0000-0000-0000-000000000001\" exists in PLAN stage")
  void featureCardInPlan() {
    featureCardExists();
  }

  @State("Feature card with id \"00000000-0000-0000-0000-000000000001\" exists")
  void featureCardExistsSimple() {
    featureCardExists();
  }

  @State("Feature card with id \"00000000-0000-0000-0000-000000000999\" does not exist")
  void featureCardDoesNotExist() {
    ensureBoardForTenant(TENANT_A_ID);
    storeApiKey(TENANT_A_ID, TEST_API_KEY);
  }

  @State("Feature card \"00000000-0000-0000-0000-000000000001\""
      + " has only one pending task \"00000000-0000-0000-0000-000000000002\"")
  void cardWithOnePendingTask() {
    featureCardExists();
    ensureTask(TENANT_A_ID, FEATURE_UUID, TASK_UUID, false);
  }

  @State("Task \"00000000-0000-0000-0000-000000000002\" is already completed")
  void taskAlreadyCompleted() {
    featureCardExists();
    ensureTask(TENANT_A_ID, FEATURE_UUID, TASK_UUID, true);
  }

  @State("Tenant has 2 features in BACKLOG")
  void tenantHasTwoFeaturesInBacklog() {
    ensureBoardForTenant(TENANT_A_ID);
    storeApiKey(TENANT_A_ID, TEST_API_KEY);
    ensureFeatureCard(TENANT_A_ID, UUID.randomUUID());
    ensureFeatureCard(TENANT_A_ID, UUID.randomUUID());
  }

  @State("Tenant has no features in SPECIFY")
  void tenantHasNoFeaturesInSpecify() {
    ensureBoardForTenant(TENANT_A_ID);
    storeApiKey(TENANT_A_ID, TEST_API_KEY);
  }

  @State("Feature \"00000000-0000-0000-0000-000000000001\""
      + " belongs to tenant A; request uses tenant B's API key")
  void crossTenantRequest() {
    featureCardExists();
    ensureBoardForTenant(TENANT_B_ID);
    storeApiKey(TENANT_B_ID, TENANT_B_KEY);
  }

  private void ensureBoardForTenant(UUID tenantId) {
    if (boardRepository.findByTenantId(tenantId).isEmpty()) {
      boardRepository.save(new Board(tenantId, "Pact Test Board"));
    }
  }

  private void storeApiKey(UUID tenantId, String rawKey) {
    String hash = sha256(rawKey);
    if (tenantApiKeyRepository.findByKeyHashAndRevokedAtIsNull(hash).isEmpty()) {
      tenantApiKeyRepository.save(new TenantApiKey(tenantId, hash));
    }
  }

  private void ensureFeatureCard(UUID tenantId, UUID cardId) {
    if (featureCardRepository.findById(cardId).isEmpty()) {
      FeatureCard card = new FeatureCard(tenantId, getBacklogColumnId(tenantId),
          "Pact Feature", null, 0);
      setCardId(card, cardId);
      featureCardRepository.save(card);
    }
  }

  private void ensureTask(UUID tenantId, UUID featureCardId, UUID taskId, boolean completed) {
    if (taskRepository.findById(taskId).isEmpty()) {
      Task task = new Task(tenantId, featureCardId, "Pact Task", null, "P1");
      setTaskId(task, taskId);
      if (completed) {
        task.complete();
      }
      taskRepository.save(task);
    }
  }

  private UUID getBacklogColumnId(UUID tenantId) {
    return columnDefRepository.findByTenantIdAndStage(tenantId, Stage.BACKLOG)
        .map(c -> c.getId())
        .orElseThrow(() -> new IllegalStateException("No BACKLOG column for tenant " + tenantId));
  }

  private static String sha256(String input) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void setCardId(FeatureCard card, UUID id) {
    try {
      var f = FeatureCard.class.getDeclaredField("id");
      f.setAccessible(true);
      f.set(card, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void setTaskId(Task task, UUID id) {
    try {
      var f = Task.class.getDeclaredField("id");
      f.setAccessible(true);
      f.set(task, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
