package com.agentboard.commons.domain;

/**
 * Discriminates the type of a WorkItem on the unified board.
 *
 * <p>Each type has its own status lifecycle and hierarchy constraints.
 */
public enum WorkItemType {

  /** Top-level spec-driven work item advancing through 9 FeatureStage values. */
  FEATURE,

  /** Second-level item attached to a FEATURE, advancing through 3 UserStoryStage values. */
  USER_STORY,

  /** Leaf-level item attached to a USER_STORY, advancing through TaskStatus (NEW/ACTIVE/CLOSED). */
  TASK
}
