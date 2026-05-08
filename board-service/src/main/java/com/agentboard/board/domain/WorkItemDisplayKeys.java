package com.agentboard.board.domain;

import com.agentboard.commons.domain.WorkItemType;

/** Human-readable display keys for work items (format: F/U/T + positive integer). */
public final class WorkItemDisplayKeys {

  private WorkItemDisplayKeys() {}

  /**
   * Returns the single-character type prefix for the given work item type.
   *
   * @param type work item discriminator
   * @return {@code F} for FEATURE, {@code U} for USER_STORY, {@code T} for TASK
   */
  public static String prefix(WorkItemType type) {
    return switch (type) {
      case FEATURE -> "F";
      case USER_STORY -> "U";
      case TASK -> "T";
    };
  }

  /**
   * Builds the display key from a type prefix and a positive sequential integer.
   *
   * @param type work item discriminator
   * @param seq  positive integer (≥ 1); the next available sequence for (project, type)
   * @return e.g. {@code F1}, {@code U102}, {@code T1023}
   */
  public static String format(WorkItemType type, int seq) {
    return prefix(type) + seq;
  }
}
