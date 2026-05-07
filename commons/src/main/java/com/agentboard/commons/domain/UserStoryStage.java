package com.agentboard.commons.domain;

/**
 * Lifecycle stages of a UserStory on the US Board.
 *
 * <p>{@link #DONE} is terminal per UserStory — no transition back once reached.
 */
public enum UserStoryStage {

  READY,
  IN_PROGRESS,
  /** Auto-only: set when all Tasks of the UserStory are completed. */
  DONE
}
