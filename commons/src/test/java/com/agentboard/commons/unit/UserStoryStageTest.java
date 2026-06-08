package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.domain.UserStoryStage;
import org.junit.jupiter.api.Test;

class UserStoryStageTest {

  @Test
  void shouldHaveThreeValues_whenValuesReturnAllStages() {
    assertThat(UserStoryStage.values()).hasSize(3);
  }

  @Test
  void shouldContainExpectedStages_whenCheckingNames() {
    assertThat(UserStoryStage.READY.name()).isEqualTo("READY");
    assertThat(UserStoryStage.IN_PROGRESS.name()).isEqualTo("IN_PROGRESS");
    assertThat(UserStoryStage.DONE.name()).isEqualTo("DONE");
  }

  @Test
  void shouldReturnEnumFromName_whenValueOfCalled() {
    assertThat(UserStoryStage.valueOf("READY")).isEqualTo(UserStoryStage.READY);
    assertThat(UserStoryStage.valueOf("IN_PROGRESS")).isEqualTo(UserStoryStage.IN_PROGRESS);
    assertThat(UserStoryStage.valueOf("DONE")).isEqualTo(UserStoryStage.DONE);
  }
}
