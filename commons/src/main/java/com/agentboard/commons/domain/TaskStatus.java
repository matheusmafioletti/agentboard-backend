package com.agentboard.commons.domain;

/**
 * Lifecycle statuses of a TASK-type WorkItem.
 *
 * <p>NOTE: NEW = not started, ACTIVE = in progress or blocked, CLOSED = done.
 * Status CLOSED triggers an auto-transition on the parent USER_STORY when all sibling
 * TASKs are also CLOSED.
 */
public enum TaskStatus {

  NEW,
  ACTIVE,
  CLOSED
}
