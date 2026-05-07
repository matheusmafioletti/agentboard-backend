package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a project that scopes all Features, UserStories, and Tasks for one agent context.
 *
 * <p>The {@link #apiKey} is generated server-side with an {@code agb_} prefix and acts as the
 * sole credential for MCP tool calls. The {@link #constitutionContent} holds the project's
 * governance rules, surfaced via the {@code get_constitution} MCP tool.
 */
@Entity
@Table(name = "project")
public class Project {

  private static final String DEFAULT_CONSTITUTION =
      "# Project Constitution\n\n"
          + "## Principles\n\n"
          + "- Quality first\n"
          + "- Test-driven development\n"
          + "- Continuous delivery\n";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "constitution_content", columnDefinition = "TEXT")
  private String constitutionContent;

  @Column(name = "api_key", nullable = false, unique = true, length = 255)
  private String apiKey;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Required by JPA. */
  protected Project() {}

  /**
   * Creates a new Project.
   *
   * @param tenantId            the tenant that owns this project
   * @param name                a unique name within the tenant
   * @param constitutionContent optional initial constitution; defaults to a template if null
   * @param apiKey              pre-generated API key (must already have the {@code agb_} prefix)
   */
  public Project(UUID tenantId, String name, String constitutionContent, String apiKey) {
    this.tenantId = tenantId;
    this.name = name;
    this.constitutionContent =
        constitutionContent != null ? constitutionContent : DEFAULT_CONSTITUTION;
    this.apiKey = apiKey;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /** Returns the project's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the owning tenant's identifier. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the project name (unique within the tenant). */
  public String getName() {
    return name;
  }

  /** Returns the project constitution content. */
  public String getConstitutionContent() {
    return constitutionContent;
  }

  /** Returns the project-scoped MCP API key. */
  public String getApiKey() {
    return apiKey;
  }

  /** Returns the instant this project was created. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the instant this project was last modified. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Replaces the constitution content. */
  public void updateConstitution(String newContent) {
    this.constitutionContent = newContent;
  }

  /** Replaces the project name. */
  public void updateName(String newName) {
    this.name = newName;
  }
}
