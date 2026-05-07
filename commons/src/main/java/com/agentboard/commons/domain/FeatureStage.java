package com.agentboard.commons.domain;

/**
 * Lifecycle stages of a Feature on the Feature Board.
 *
 * <p>Manual transitions are permitted for all stages except {@link #IN_DEVELOPMENT} and
 * {@link #PR_REVIEW}, which are set exclusively via automatic events from the US Board.
 * {@link #DONE} is terminal — no transitions out of it.
 */
public enum FeatureStage {

  BACKLOG,
  SPECIFY,
  CLARIFY,
  PLAN,
  TASKS,
  /** Auto-only: set when {@code create_user_stories} completes. */
  READY,
  /** Auto-only: set when the first UserStory moves to IN_PROGRESS. */
  IN_DEVELOPMENT,
  /** Auto-only: set when the last UserStory reaches DONE. */
  PR_REVIEW,
  DONE;

  /** Returns true when this stage may only be entered via an automatic board event. */
  public boolean isAutoOnly() {
    return this == IN_DEVELOPMENT || this == PR_REVIEW;
  }
}
